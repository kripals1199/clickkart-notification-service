// src/main/java/com/clickkart/notification/exception/MissingCorrelationIdException.java
package com.clickkart.notification.exception;

/**
 * Thrown when an inbound request has no {@code X-Correlation-Id} header (Rule 13: only Auth
 * Service mints a correlation id - every other service must extract and re-propagate it, never
 * invent a substitute). Health/Swagger/actuator paths are exempt - see {@code CorrelationIdFilter}.
 */
public class MissingCorrelationIdException extends RuntimeException {

    public MissingCorrelationIdException(String message) {
        super(message);
    }
}
