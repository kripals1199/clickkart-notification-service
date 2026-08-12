// src/main/java/com/clickkart/notification/dto/request/PasswordResetNotificationRequest.java
package com.clickkart.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Matches Auth Service's {@code PasswordResetNotificationRequest} shape exactly (this service's
 * own copy, per Rule 4 - no shared library). {@code rawResetToken} is the one and only place
 * this raw value ever appears after being minted - never persisted, only used to render the
 * simulated dispatch content.
 */
public record PasswordResetNotificationRequest(
        @NotBlank @Email String recipientEmail, @NotBlank String rawResetToken, @NotNull Instant expiresAt) {}
