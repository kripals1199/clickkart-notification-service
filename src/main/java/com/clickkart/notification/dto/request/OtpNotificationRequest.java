// src/main/java/com/clickkart/notification/dto/request/OtpNotificationRequest.java
package com.clickkart.notification.dto.request;

import com.clickkart.notification.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Matches Auth Service's {@code OtpNotificationRequest} shape exactly (this service's own copy).
 * Covers both login OTPs and email/mobile verification codes - the caller doesn't distinguish
 * them at the wire level, both are "a short code to deliver". Exactly one of
 * {@code recipientEmail}/{@code recipientMobileNumber} is expected to be populated, matching
 * {@code channel}; enforced in the service layer, not as a Bean Validation cross-field
 * constraint, to keep the DTO itself simple.
 */
public record OtpNotificationRequest(
        @NotNull NotificationChannel channel,
        String recipientEmail,
        String recipientMobileNumber,
        @NotBlank String rawOtp,
        @NotNull Instant expiresAt) {}
