// src/main/java/com/clickkart/notification/service/NotificationDispatchService.java
package com.clickkart.notification.service;

import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;

/**
 * Owns "sending" a notification. Currently a simulated dispatch (logs the rendered content at
 * INFO and persists a durable {@code NotificationEntity} record - no real SMTP/SMS provider
 * call) - see the class Javadoc on {@code NotificationDispatchServiceImpl} for the swap-in point
 * a real provider integration would replace.
 */
public interface NotificationDispatchService {

    void dispatchPasswordReset(String correlationId, PasswordResetNotificationRequest request);

    void dispatchOtp(String correlationId, OtpNotificationRequest request);
}
