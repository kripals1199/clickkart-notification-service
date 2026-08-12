// src/main/java/com/clickkart/notification/controller/NotificationController.java
package com.clickkart.notification.controller;

import com.clickkart.notification.constant.ApiPaths;
import com.clickkart.notification.constant.MdcKeys;
import com.clickkart.notification.dto.ApiResponse;
import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;
import com.clickkart.notification.filter.CorrelationIdFilter;
import com.clickkart.notification.service.NotificationDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by other services (Auth Service today) to dispatch a password-reset link or a
 * numeric code (login OTP / email-mobile verification). Internal, service-to-service surface -
 * no user-facing endpoint reaches this directly, so there is deliberately no RBAC here, matching
 * this service's private-network-only deployment assumption (see {@code
 * k8s/notification-service/service-and-scaling.yaml} - ClusterIP only).
 */
@Tag(name = "Notifications", description = "Simulated dispatch of password-reset/OTP notifications")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationDispatchService notificationDispatchService;

    /** 200 OK, {@code data: null}. Matches Auth Service's {@code NotificationServiceClient.sendPasswordResetNotification}. */
    @Operation(summary = "Dispatch a password-reset notification")
    @PostMapping(ApiPaths.PASSWORD_RESET)
    public ResponseEntity<ApiResponse<Void>> passwordReset(
            @RequestHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) String correlationId,
            @Valid @RequestBody PasswordResetNotificationRequest request,
            HttpServletRequest httpRequest) {
        notificationDispatchService.dispatchPasswordReset(correlationId, request);
        return envelope(HttpStatus.OK.value(), httpRequest);
    }

    /** 200 OK, {@code data: null}. Matches Auth Service's {@code NotificationServiceClient.sendOtp}. */
    @Operation(summary = "Dispatch an OTP/verification-code notification")
    @PostMapping(ApiPaths.OTP)
    public ResponseEntity<ApiResponse<Void>> otp(
            @RequestHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) String correlationId,
            @Valid @RequestBody OtpNotificationRequest request,
            HttpServletRequest httpRequest) {
        notificationDispatchService.dispatchOtp(correlationId, request);
        return envelope(HttpStatus.OK.value(), httpRequest);
    }

    private ResponseEntity<ApiResponse<Void>> envelope(int status, HttpServletRequest request) {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        ApiResponse<Void> body = ApiResponse.success(status, null, request.getRequestURI(), correlationId);
        return ResponseEntity.status(status).body(body);
    }
}
