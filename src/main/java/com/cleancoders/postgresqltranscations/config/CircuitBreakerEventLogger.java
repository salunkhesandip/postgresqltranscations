package com.cleancoders.postgresqltranscations.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscribes to Resilience4j {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} events
 * and logs every state transition (CLOSED → OPEN → HALF_OPEN → CLOSED).
 *
 * <p>No metrics stack required — uses SLF4J only.
 */
@Component
public class CircuitBreakerEventLogger {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventLogger.class);

    public CircuitBreakerEventLogger(CircuitBreakerRegistry registry) {
        registry.getAllCircuitBreakers().forEach(cb ->
                cb.getEventPublisher()
                        .onStateTransition(event -> log.warn(
                                "CircuitBreaker [{}] state transition: {} → {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onCallNotPermitted(event -> log.warn(
                                "CircuitBreaker [{}] is OPEN — call rejected",
                                event.getCircuitBreakerName()))
                        .onError(event -> log.error(
                                "CircuitBreaker [{}] recorded a failure: {}",
                                event.getCircuitBreakerName(),
                                event.getThrowable().getMessage()))
                        .onSuccess(event -> log.debug(
                                "CircuitBreaker [{}] recorded a success (duration: {})",
                                event.getCircuitBreakerName(),
                                event.getElapsedDuration()))
        );
    }
}

