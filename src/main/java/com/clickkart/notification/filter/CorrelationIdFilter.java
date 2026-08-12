// src/main/java/com/clickkart/notification/filter/CorrelationIdFilter.java
package com.clickkart.notification.filter;

import com.clickkart.notification.constant.MdcKeys;
import com.clickkart.notification.exception.MissingCorrelationIdException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Unlike Auth Service (the only correlation-id *minter*, Rule 13), this service is purely a
 * *receiver*: every inbound request must already carry {@code X-Correlation-Id} - set by Auth
 * Service's Feign clients today, and by the Gateway for anything routed through it in future.
 * This service never invents a substitute; a missing/blank header is rejected outright
 * ({@link MissingCorrelationIdException} -> 400), same posture Auth Service's own {@code
 * JwtAuthenticationFilter} takes toward a token missing its correlationId claim.
 *
 * <p>Health/Swagger/actuator paths are exempt - k8s probes and API-doc scrapers have no
 * correlation id to send and none is meaningful for them.
 */
@RequiredArgsConstructor
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final List<String> exemptPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isExempt(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            handlerExceptionResolver.resolveException(
                    request, response, null,
                    new MissingCorrelationIdException("Request is missing the required X-Correlation-Id header"));
            return;
        }

        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
        }
    }

    private boolean isExempt(String path) {
        for (String pattern : exemptPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
