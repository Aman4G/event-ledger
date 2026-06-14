package com.eventledger.gateway.interfaces.health;

import com.eventledger.gateway.common.model.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final HealthEndpoint healthEndpoint;

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