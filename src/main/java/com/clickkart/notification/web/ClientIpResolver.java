// src/main/java/com/clickkart/notification/web/ClientIpResolver.java
package com.clickkart.notification.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Single source of truth for resolving a caller's IP for logging/audit context.
 *
 * <p>Unlike Auth Service's rate-limiting-driving resolver, no security decision here depends on
 * this value (this service does no rate limiting), so {@code X-Forwarded-For} is trusted at face
 * value purely for log/audit readability - a forged value only misleads a log line, it can't be
 * used to bypass any control.
 */
public final class ClientIpResolver {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(X_FORWARDED_FOR_HEADER))
                .filter(header -> !header.isBlank())
                .map(header -> header.split(",")[0].trim())
                .orElseGet(request::getRemoteAddr);
    }
}
