package com.eventledger.account.interfaces.health;

import com.eventledger.account.interfaces.health.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: HealthController.java exposes the GET /health endpoint for the Account Service.
 * It queries Spring Boot Actuator's HealthEndpoint to determine database connectivity status
 * and returns a structured response containing the service name, overall status, current
 * timestamp, and diagnostic details.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    /**
     * Returns the current health status of the Account Service, including database
     * connectivity as reported by Spring Boot Actuator.
     *
     * @return HealthResponse containing service name, status UP, current timestamp,
     *         and a diagnostics map with database status and description
     */
    @GetMapping("/health")
    public HealthResponse health() {
        log.info("Health check requested");

        String dbStatus = healthEndpoint.health().getStatus().getCode();

        return HealthResponse.builder()
                .service("account-service")
                .status("UP")
                .timestamp(Instant.now())
                .diagnostics(Map.of(
                        "database", dbStatus,
                        "description", "Account Service is running"
                ))
                .build();
    }
}