package com.eventledger.account.interfaces.health;

import com.eventledger.account.interfaces.health.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthEndpoint healthEndpoint;

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