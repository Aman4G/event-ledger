package com.eventledger.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Description: Resilience4jConfig.java wires the circuit breaker configuration properties
 * defined in application.properties into the Spring Cloud CircuitBreakerFactory via a
 * Customizer bean. Without this, the resilience4j.circuitbreaker.instances.* and
 * resilience4j.timelimiter.instances.* properties are silently ignored by the
 * Spring Cloud abstraction layer and the circuit breaker runs on default settings.
 */
@Configuration
public class Resilience4jConfig {

    @Value("${resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.failure-rate-threshold}")
    private float failureRateThreshold;

    @Value("${resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.minimum-number-of-calls}")
    private int minimumNumberOfCalls;

    @Value("${resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.sliding-window-size}")
    private int slidingWindowSize;

    @Value("${resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.wait-duration-in-open-state}")
    private Duration waitDurationInOpenState;

    @Value("${resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.permitted-number-of-calls-in-half-open-state}")
    private int permittedNumberOfCallsInHalfOpenState;

    @Value("${resilience4j.timelimiter.instances.accountServiceCircuitBreaker.timeout-duration}")
    private Duration timeoutDuration;

    /**
     * Registers a Customizer that applies the circuit breaker and time limiter configuration
     * from application.properties to the named circuit breaker instance used by
     * AccountServiceClientImpl. The instance name must match the one passed to
     * circuitBreakerFactory.create() in code.
     *
     * @return Customizer that configures the accountServiceCircuitBreaker instance
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> accountServiceCircuitBreakerCustomizer() {

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .slidingWindowSize(slidingWindowSize)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeoutDuration)
                .build();

        return factory -> factory.configure(
                builder -> builder
                        .circuitBreakerConfig(circuitBreakerConfig)
                        .timeLimiterConfig(timeLimiterConfig),
                "accountServiceCircuitBreaker"
        );
    }
}
