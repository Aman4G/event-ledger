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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration Test: Gateway → Account Service Flow
 *
 * This test exercises the complete flow:
 * 1. Submit an event to the Gateway service (/events endpoint)
 * 2. Gateway saves the event to its database
 * 3. Gateway calls the Account Service to apply the transaction
 * 4. Account Service processes the transaction
 * 5. Response flows back to the client
 *
 * The Account Service is mocked using WireMock to simulate the actual service behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GatewayToAccountServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static WireMockServer accountServiceMock;

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

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (accountServiceMock == null) {
            accountServiceMock = new WireMockServer(wireMockConfig().dynamicPort());
            accountServiceMock.start();
        }
        registry.add("account.service.base-url", () -> "http://localhost:" + accountServiceMock.port());
    }

    @Test
    void testFullGatewayToAccountServiceFlow_SuccessfulTransaction() throws Exception {
        // Arrange
        String accountId = "ACC-001";
        String eventId = "EVT-001";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        Instant eventTimestamp = Instant.now();
        EventType transactionType = EventType.CREDIT;

        EventRequest eventRequest = new EventRequest();

        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(transactionType);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency(currency);
        eventRequest.setEventTimestamp(eventTimestamp);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "payment_system");
        metadata.put("reference", "REF-123");

        eventRequest.setMetadata(metadata);

        // Mock the Account Service response
        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("eventId", eventId);
        accountServiceResponse.put("accountId", accountId);
        accountServiceResponse.put("type", transactionType.toString());
        accountServiceResponse.put("amount", amount);
        accountServiceResponse.put("currency", currency);
        accountServiceResponse.put("eventTimestamp", eventTimestamp);
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                )
        );

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .header("X-Trace-Id", "integration-trace-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(eventRequest))
                )
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.eventId").value(eventId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.accountId").value(accountId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.type").value(transactionType.toString()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(amount.intValue()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(currency))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACCEPTED"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.source").value("payment_system"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.reference").value("REF-123"));

        // Verify that the Account Service was called with correct payload
        accountServiceMock.verify(
                postRequestedFor(urlEqualTo("/accounts/" + accountId + "/transactions"))
                        .withHeader("Content-Type", containing("application/json"))
                        .withHeader("X-Trace-Id", equalTo("integration-trace-001"))
        );
    }

    @Test
    void testFullGatewayToAccountServiceFlow_DuplicateEventIgnored() throws Exception {
        // Arrange
        String accountId = "ACC-002";
        String eventId = "EVT-002";
        BigDecimal amount = new BigDecimal("50.00");
        String currency = "USD";
        Instant eventTimestamp = Instant.now();
        EventType transactionType = EventType.DEBIT;

        EventRequest eventRequest = new EventRequest();

        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(transactionType);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency(currency);
        eventRequest.setEventTimestamp(eventTimestamp);

        // Mock the Account Service response
        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("eventId", eventId);
        accountServiceResponse.put("accountId", accountId);
        accountServiceResponse.put("type", transactionType.toString());
        accountServiceResponse.put("amount", amount);
        accountServiceResponse.put("currency", currency);
        accountServiceResponse.put("eventTimestamp", eventTimestamp);
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                )
        );

        // First submission - should succeed
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .header("X-Trace-Id", "integration-trace-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(eventRequest))
                )
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACCEPTED"));

        // Second submission with same eventId - should be ignored
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest))
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DUPLICATE_IGNORED"));

        // Verify Account Service was called only once (not twice)
        accountServiceMock.verify(1, postRequestedFor(urlEqualTo("/accounts/" + accountId + "/transactions")));
    }

    @Test
    void testFullGatewayToAccountServiceFlow_AccountServiceUnavailable() throws Exception {
        // Arrange
        String accountId = "ACC-003";
        String eventId = "EVT-003";
        BigDecimal amount = new BigDecimal("75.50");
        String currency = "EUR";
        Instant eventTimestamp = Instant.now();
        EventType transactionType = EventType.CREDIT;

        EventRequest eventRequest = new EventRequest();

        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(transactionType);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency(currency);
        eventRequest.setEventTimestamp(eventTimestamp);

        // Mock Account Service to return 500 error
        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(serverError())
        );

        // Act & Assert - Should fail with service unavailable
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .header("X-Trace-Id", "integration-trace-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(eventRequest))
                )
        .andExpect(MockMvcResultMatchers.status().isServiceUnavailable())
        .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    void testFullGatewayToAccountServiceFlow_InvalidEventData() throws Exception {
        // Arrange - Missing required field (accountId)
        Map<String, Object> eventRequest = new HashMap<>();
        eventRequest.put("eventId", "EVT-004");
        eventRequest.put("type", EventType.CREDIT.toString());
        eventRequest.put("amount", new BigDecimal("100.00"));
        eventRequest.put("currency", "USD");
        eventRequest.put("eventTimestamp", Instant.now());
        // accountId is missing - should fail validation

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .header("X-Trace-Id", "integration-trace-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(eventRequest))
                )
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // Verify Account Service was NOT called
        accountServiceMock.verify(0, postRequestedFor(urlMatching("/accounts/.*/transactions")));
    }

    @Test
    void testFullGatewayToAccountServiceFlow_MultipleTransactionTypes() throws Exception {
        // Arrange - Test DEBIT transaction
        String accountId = "ACC-004";
        String eventId = "EVT-005";
        BigDecimal amount = new BigDecimal("250.00");
        String currency = "GBP";
        Instant eventTimestamp = Instant.now();
        EventType transactionType = EventType.DEBIT;

        EventRequest eventRequest = new EventRequest();

        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(transactionType);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency(currency);
        eventRequest.setEventTimestamp(eventTimestamp);

        // Mock the Account Service response
        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("eventId", eventId);
        accountServiceResponse.put("accountId", accountId);
        accountServiceResponse.put("type", transactionType.toString());
        accountServiceResponse.put("amount", amount);
        accountServiceResponse.put("currency", currency);
        accountServiceResponse.put("eventTimestamp", eventTimestamp);
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                )
        );

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .header("X-Trace-Id", "integration-trace-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(eventRequest))
                )
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.eventId").value(eventId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.accountId").value(accountId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.type").value(EventType.DEBIT.toString()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(amount.intValue()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(currency));
    }

    @Test
    void testGetEventById_EventExists() throws Exception {
        // Arrange
        String accountId = "ACC-005";
        String eventId = "EVT-006";
        BigDecimal amount = new BigDecimal("175.50");
        String currency = "INR";
        Instant eventTimestamp = Instant.now();
        EventType transactionType = EventType.CREDIT;

        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(transactionType);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency(currency);
        eventRequest.setEventTimestamp(eventTimestamp);

        // First, create an event
        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("eventId", eventId);
        accountServiceResponse.put("accountId", accountId);
        accountServiceResponse.put("type", transactionType.toString());
        accountServiceResponse.put("amount", amount);
        accountServiceResponse.put("currency", currency);
        accountServiceResponse.put("eventTimestamp", eventTimestamp);
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                )
        );

        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .header("X-Trace-Id", "integration-trace-006")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest))
        );

        // Act & Assert - Retrieve the event by ID
        mockMvc.perform(
            MockMvcRequestBuilders.get("/events/" + eventId)
                .header("X-Trace-Id", "integration-trace-006")
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.eventId").value(eventId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.accountId").value(accountId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.type").value(transactionType.toString()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(amount.intValue()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(currency))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("FOUND"));
    }

    @Test
    void testGetEventById_EventNotFound() throws Exception {
        // Arrange
        String nonExistentEventId = "EVT-NONEXISTENT";

        // Act & Assert
        mockMvc.perform(
            MockMvcRequestBuilders.get("/events/" + nonExistentEventId)
                .header("X-Trace-Id", "integration-trace-007")
        )
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testGetEventsByAccount_ReturnsAllEventsForAccount() throws Exception {
        // Arrange
        String accountId = "ACC-006";
        EventType transactionType1 = EventType.CREDIT;
        EventType transactionType2 = EventType.DEBIT;

        // Create first event
        String eventId1 = "EVT-007";
        BigDecimal amount1 = new BigDecimal("100.00");
        EventRequest eventRequest1 = new EventRequest();
        eventRequest1.setEventId(eventId1);
        eventRequest1.setAccountId(accountId);
        eventRequest1.setType(transactionType1);
        eventRequest1.setAmount(amount1);
        eventRequest1.setCurrency("USD");
        eventRequest1.setEventTimestamp(Instant.now());

        // Create second event
        String eventId2 = "EVT-008";
        BigDecimal amount2 = new BigDecimal("50.00");
        EventRequest eventRequest2 = new EventRequest();
        eventRequest2.setEventId(eventId2);
        eventRequest2.setAccountId(accountId);
        eventRequest2.setType(transactionType2);
        eventRequest2.setAmount(amount2);
        eventRequest2.setCurrency("USD");
        eventRequest2.setEventTimestamp(Instant.now());

        // Mock Account Service responses
        Map<String, Object> response1 = new HashMap<>();
        response1.put("eventId", eventId1);
        response1.put("accountId", accountId);
        response1.put("type", transactionType1.toString());
        response1.put("amount", amount1);
        response1.put("currency", "USD");
        response1.put("status", "APPLIED");

        Map<String, Object> response2 = new HashMap<>();
        response2.put("eventId", eventId2);
        response2.put("accountId", accountId);
        response2.put("type", transactionType2.toString());
        response2.put("amount", amount2);
        response2.put("currency", "USD");
        response2.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response1))
                )
        );

        // Submit first event
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .header("X-Trace-Id", "integration-trace-008")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest1))
        );

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response2))
                )
        );

        // Submit second event
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .header("X-Trace-Id", "integration-trace-008")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest2))
        );

        // Act & Assert - Retrieve all events for account
        mockMvc.perform(
            MockMvcRequestBuilders.get("/events")
                .param("account", accountId)
                .header("X-Trace-Id", "integration-trace-008")
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].eventId").value(eventId1))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].accountId").value(accountId))
        .andExpect(MockMvcResultMatchers.jsonPath("$[1].eventId").value(eventId2))
        .andExpect(MockMvcResultMatchers.jsonPath("$[1].accountId").value(accountId));
    }

    @Test
    void testGetAccountBalance_FromAccountService() throws Exception {
        // Arrange
        String accountId = "ACC-007";
        BigDecimal expectedBalance = new BigDecimal("500.00");

        Map<String, Object> balanceResponse = new HashMap<>();
        balanceResponse.put("accountId", accountId);
        balanceResponse.put("balance", expectedBalance);

        accountServiceMock.stubFor(
            WireMock.get(urlEqualTo("/accounts/" + accountId + "/balance"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(balanceResponse))
                )
        );

        // Act & Assert - Gateway calls Account Service to get balance
        mockMvc.perform(
            MockMvcRequestBuilders.get("/accounts/" + accountId + "/balance")
                .header("X-Trace-Id", "integration-trace-009")
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.accountId").value(accountId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.balance").value(expectedBalance.intValue()));

        // Verify Account Service was called
        accountServiceMock.verify(
            getRequestedFor(urlEqualTo("/accounts/" + accountId + "/balance"))
                .withHeader("X-Trace-Id", equalTo("integration-trace-009"))
        );
    }

    @Test
    void testGetAccountBalance_AccountServiceUnavailable() throws Exception {
        // Arrange
        String accountId = "ACC-008";

        accountServiceMock.stubFor(
            WireMock.get(urlEqualTo("/accounts/" + accountId + "/balance"))
                .willReturn(serverError())
        );

        // Act & Assert
        mockMvc.perform(
            MockMvcRequestBuilders.get("/accounts/" + accountId + "/balance")
                .header("X-Trace-Id", "integration-trace-010")
        )
        .andExpect(MockMvcResultMatchers.status().isServiceUnavailable())
        .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    void testGatewayHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(
            MockMvcRequestBuilders.get("/health")
                .header("X-Trace-Id", "integration-trace-011")
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.service").value("event-gateway-service"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.diagnostics.database").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists());
    }

    @Test
    void testTraceIdPropagationAcrossServices() throws Exception {
        // Arrange
        String customTraceId = "custom-trace-id-12345";
        String accountId = "ACC-009";
        String eventId = "EVT-009";
        BigDecimal amount = new BigDecimal("99.99");
        Instant eventTimestamp = Instant.now();

        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(eventId);
        eventRequest.setAccountId(accountId);
        eventRequest.setType(EventType.CREDIT);
        eventRequest.setAmount(amount);
        eventRequest.setCurrency("USD");
        eventRequest.setEventTimestamp(eventTimestamp);

        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("eventId", eventId);
        accountServiceResponse.put("accountId", accountId);
        accountServiceResponse.put("type", "CREDIT");
        accountServiceResponse.put("amount", amount);
        accountServiceResponse.put("currency", "USD");
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
            WireMock.post(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                )
        );

        // Act
        mockMvc.perform(
            MockMvcRequestBuilders.post("/events")
                .header("X-Trace-Id", customTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest))
        )
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.header().exists("X-Trace-Id"));

        // Assert - Verify that the trace ID was propagated to Account Service
        accountServiceMock.verify(
            postRequestedFor(urlEqualTo("/accounts/" + accountId + "/transactions"))
                .withHeader("X-Trace-Id", equalTo(customTraceId))
        );
    }

    @Test
    void testGetEventsByAccount_ReturnsEventsOrderedByTimestamp() throws Exception {

        String accountId = "ACC-ORDER-001";

        Instant newerTimestamp = Instant.parse("2026-06-14T11:00:00Z");
        Instant olderTimestamp = Instant.parse("2026-06-14T09:00:00Z");

        EventRequest newerEvent = new EventRequest();
        newerEvent.setEventId("EVT-NEWER");
        newerEvent.setAccountId(accountId);
        newerEvent.setType(EventType.CREDIT);
        newerEvent.setAmount(new BigDecimal("100.00"));
        newerEvent.setCurrency("USD");
        newerEvent.setEventTimestamp(newerTimestamp);

        EventRequest olderEvent = new EventRequest();
        olderEvent.setEventId("EVT-OLDER");
        olderEvent.setAccountId(accountId);
        olderEvent.setType(EventType.DEBIT);
        olderEvent.setAmount(new BigDecimal("25.00"));
        olderEvent.setCurrency("USD");
        olderEvent.setEventTimestamp(olderTimestamp);

        Map<String, Object> accountServiceResponse = new HashMap<>();
        accountServiceResponse.put("status", "APPLIED");

        accountServiceMock.stubFor(
                WireMock.post(urlMatching("/accounts/.*/transactions"))
                        .willReturn(
                                ok()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(objectMapper.writeValueAsString(accountServiceResponse))
                        )
        );

        // Submit NEWER event first
        mockMvc.perform(
                MockMvcRequestBuilders.post("/events")
                        .header("X-Trace-Id", "integration-trace-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newerEvent))
        ).andExpect(MockMvcResultMatchers.status().isCreated());

        // Submit OLDER event later
        mockMvc.perform(
                MockMvcRequestBuilders.post("/events")
                        .header("X-Trace-Id", "integration-trace-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olderEvent))
        ).andExpect(MockMvcResultMatchers.status().isCreated());

        // Verify retrieval order is chronological
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/events")
                                .param("account", accountId)
                                .header("X-Trace-Id", "integration-trace-order")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))

                // older first
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].eventId").value("EVT-OLDER"))

                // newer second
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].eventId").value("EVT-NEWER"));
    }
}
