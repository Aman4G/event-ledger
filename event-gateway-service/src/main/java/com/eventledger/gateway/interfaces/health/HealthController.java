package com.eventledger.gateway.interfaces.health;

import com.eventledger.gateway.common.model.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Description: HealthController.java exposes the GET /health endpoint for the Event Gateway
 * Service. It queries Spring Boot Actuator's HealthEndpoint to determine database connectivity
 * status and returns a structured response containing the service name, overall status,
 * current timestamp, and diagnostic details.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    /**
     * Returns the current health status of the Event Gateway Service, including database
     * connectivity as reported by Spring Boot Actuator.
     *
     * @return HealthResponse containing service name, status UP, current timestamp,
     *         and a diagnostics map with database status and description
     */
    @GetMapping("/health")
    public HealthResponse health() {
        log.info("Gateway health check requested");

        String dbStatus = healthEndpoint.health().getStatus().getCode();

        return HealthResponse.builder()
                .service("event-gateway-service")
                .status("UP")
                .timestamp(Instant.now())
                .diagnostics(Map.of(
                        "database", dbStatus,
                        "description", "Event Gateway Service is running"
                ))
                .build();
    }
}