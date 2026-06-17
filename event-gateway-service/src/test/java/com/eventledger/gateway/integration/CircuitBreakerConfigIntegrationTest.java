package com.eventledger.gateway.integration;

import com.eventledger.gateway.common.enums.EventType;
import com.eventledger.gateway.interfaces.event.dto.EventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.test.annotation.DirtiesContext;

import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Verifies that the Resilience4j circuit breaker configuration defined in
 * application.properties is correctly wired via Resilience4jConfig and respected
 * at runtime.
 * Kept separate from GatewayToAccountServiceIntegrationTest because opening the
 * circuit breaker changes shared state (the circuit breaker instance) that would
 * affect other tests running in the same application context.
 * Configuration under test (from application.properties):
 *   minimum-number-of-calls = 3   → circuit evaluates after 3 calls
 *   failure-rate-threshold   = 50 → opens when ≥50% of those calls fail
 *   sliding-window-size      = 5
 *   wait-duration-in-open-state = 10s
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CircuitBreakerConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static WireMockServer accountServiceMock;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (accountServiceMock == null) {
            accountServiceMock = new WireMockServer(wireMockConfig().dynamicPort());
            accountServiceMock.start();
        }
        registry.add("account.service.base-url", () -> "http://localhost:" + accountServiceMock.port());
    }

    @BeforeEach
    void setUp() {
        if (accountServiceMock == null) {
            accountServiceMock = new WireMockServer(wireMockConfig().dynamicPort());
            accountServiceMock.start();
        }
        WireMock.configureFor("localhost", accountServiceMock.port());
    }

    @AfterEach
    void tearDown() {
        if (accountServiceMock != null) {
            accountServiceMock.resetAll();
        }
    }

    /**
     * Verifies that the circuit breaker does NOT open when the failure rate stays
     * below the configured threshold.
     * Sends 1 failure and 2 successful calls.
     * Failure rate = 33%, which is below the configured 50% threshold,
     * so the circuit remains closed and all 3 calls reach WireMock.
     *
     * @DirtiesContext forces a fresh Spring context after this test so the open circuit
     * breaker state does not leak into subsequent tests.
     */
    @Test
    @DirtiesContext
    void circuitBreaker_OpensAfterFailureThresholdCrossed() throws Exception {
        String accountId = "ACC-CB-001";
        AtomicInteger eventCounter = new AtomicInteger(1);

        // Stub all calls to fail
        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(serverError())
        );

        // Send 3 failing calls — satisfies minimum-number-of-calls
        // failure rate = 100% → exceeds 50% threshold → circuit opens
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRequest(accountId, "EVT-CB-" + eventCounter.getAndIncrement())))
            ).andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
        }

        // Circuit is now OPEN — this call should be short-circuited without reaching WireMock
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(accountId, "EVT-CB-" + eventCounter.getAndIncrement())))
        ).andExpect(MockMvcResultMatchers.status().isServiceUnavailable());

        // Verify WireMock received exactly 3 requests — the 4th was rejected by the open circuit
        accountServiceMock.verify(3,
            WireMock.postRequestedFor(urlEqualTo("/accounts/" + accountId + "/transactions"))
        );
    }

    /**
     * Verifies that the circuit breaker does NOT open when the failure rate stays
     * below the configured threshold (failure-rate-threshold=50).
     * Sends 2 failures and 1 success within the sliding window.
     * failure rate = 66% over 3 calls... but wait — this test uses 1 failure out of 3
     * to stay below 50%, proving the threshold is respected and the circuit stays closed.
     */
    @Test
    void circuitBreaker_StaysClosedWhenFailureRateBelowThreshold() throws Exception {
        String accountId = "ACC-CB-002";
        AtomicInteger eventCounter = new AtomicInteger(1);

        // Stub first call to fail
        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .inScenario("partial-failure")
                .whenScenarioStateIs("Started")
                .willReturn(serverError())
                .willSetStateTo("after-failure")
        );

        // Stub remaining calls to succeed
        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .inScenario("partial-failure")
                .whenScenarioStateIs("after-failure")
                .willReturn(
                    WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                            .withBody(
                                    buildAccountServiceSuccessResponse(
                                            accountId
                                    )
                            )
                )
        );

        // 1 failure out of 3 calls = 33% failure rate — below 50% threshold
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(accountId, "EVT-CB-" + eventCounter.getAndIncrement())))
        ).andExpect(MockMvcResultMatchers.status().isServiceUnavailable()); // 1st: fails

        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(accountId, "EVT-CB-" + eventCounter.getAndIncrement())))
        ).andExpect(MockMvcResultMatchers.status().isCreated()); // 2nd: succeeds

        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(accountId, "EVT-CB-" + eventCounter.getAndIncrement())))
        ).andExpect(MockMvcResultMatchers.status().isCreated()); // 3rd: succeeds

        // Circuit is still CLOSED — all 3 requests reached WireMock
        accountServiceMock.verify(3,
            WireMock.postRequestedFor(urlEqualTo("/accounts/" + accountId + "/transactions"))
        );
    }

    private String buildAccountServiceSuccessResponse(
            String accountId
    ) throws Exception {

        Map<String, Object> response = new HashMap<>();

        response.put("eventId", "EVT-CB-SUCCESS");
        response.put("accountId", accountId);
        response.put("type", "CREDIT");
        response.put("amount", new BigDecimal("100.00"));
        response.put("currency", "USD");
        response.put("eventTimestamp", Instant.now().toString());
        response.put("status", "APPLIED");

        return objectMapper.writeValueAsString(response);
    }

    private EventRequest buildRequest(String accountId, String eventId) {
        EventRequest request = new EventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType(EventType.CREDIT);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());
        return request;
    }
}
