// src/main/java/com/clickkart/notification/serviceImpl/NotificationDispatchServiceImpl.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;
import com.clickkart.notification.entity.NotificationEntity;
import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;
import com.clickkart.notification.repository.NotificationRepository;
import com.clickkart.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simulated dispatch (user-confirmed decision, not a placeholder left for later by oversight):
 * builds the message content, logs it at INFO via the dedicated {@link LoggerNames#DISPATCH}
 * logger (this line *is* the "send" - no external SMTP/SMS provider is called), and persists a
 * durable {@link NotificationEntity} record of the attempt. Swapping in a real provider later
 * (JavaMailSender for email, a Twilio client for SMS) only touches this one class - the
 * controller/DTO/entity layers don't change.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchPasswordReset(String correlationId, PasswordResetNotificationRequest request) {
        String content = "Password reset requested. Use this token to complete the reset (expires "+ request.expiresAt() + "): " + request.rawResetToken();

        log.info(
                "SIMULATED_DISPATCH type=PASSWORD_RESET channel=EMAIL recipient={} correlationId={} content=\"{}\"",
                request.recipientEmail(), correlationId, content);

        notificationRepository.save(new NotificationEntity(request.recipientEmail(), NotificationChannel.EMAIL, NotificationType.PASSWORD_RESET, NotificationStatus.SENT, correlationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchOtp(String correlationId, OtpNotificationRequest request) {
        NotificationChannel channel = request.channel();
        String recipient = resolveRecipient(channel, request);

        String content = "Your ClickKart verification code is " + request.rawOtp()+ " (expires " + request.expiresAt() + ")";

        log.info(
                "SIMULATED_DISPATCH type=OTP channel={} recipient={} correlationId={} content=\"{}\"",
                channel, recipient, correlationId, content);

        notificationRepository.save(new NotificationEntity(recipient, channel, NotificationType.OTP, NotificationStatus.SENT, correlationId));
    }

    private String resolveRecipient(NotificationChannel channel, OtpNotificationRequest request) {
        String recipient = channel == NotificationChannel.SMS ? request.recipientMobileNumber() : request.recipientEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipientMobileNumber/recipientEmail must match the declared channel " + channel);
        }
        return recipient;
    }
}
