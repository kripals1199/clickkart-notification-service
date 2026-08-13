// src/main/java/com/clickkart/notification/serviceImpl/NotificationDispatchServiceImpl.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.config.NotificationProperties;
import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;
import com.clickkart.notification.entity.NotificationEntity;
import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;
import com.clickkart.notification.repository.NotificationRepository;
import com.clickkart.notification.service.EmailSender;
import com.clickkart.notification.service.NotificationDispatchService;
import com.clickkart.notification.service.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the message content and hands it to the configured transport ({@link EmailSender} /
 * {@link SmsSender}), then records a durable {@link NotificationEntity} of the outcome.
 *
 * <p>Whether anything is really sent depends on configuration, not on this class: with SMTP or
 * MSG91 credentials present the real senders are active, otherwise the logging fallbacks are.
 * Either way the recorded status reflects what actually happened - a send that throws is stored
 * as {@link NotificationStatus#FAILED} and the exception propagates to Auth Service, so a
 * password-reset request can never report success when the user received nothing.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final NotificationProperties notificationProperties;
    private final NotificationFailureRecorder notificationFailureRecorder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchPasswordReset(String correlationId, PasswordResetNotificationRequest request) {
        String body = "Password reset requested. Use this token to complete the reset (expires "
                + request.expiresAt() + "): " + request.rawResetToken();
        String subject = notificationProperties.getEmail().getPasswordResetSubject();

        dispatch(
                correlationId,
                request.recipientEmail(),
                NotificationChannel.EMAIL,
                NotificationType.PASSWORD_RESET,
                () -> emailSender.send(request.recipientEmail(), subject, body));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchOtp(String correlationId, OtpNotificationRequest request) {
        NotificationChannel channel = request.channel();
        String recipient = resolveRecipient(channel, request);
        String body = "Your ClickKart verification code is " + request.rawOtp()
                + " (expires " + request.expiresAt() + ")";

        Runnable send = channel == NotificationChannel.SMS
                // MSG91's DLT template supplies the surrounding wording, so only the code itself
                // is sent as the template variable - see Msg91SmsSenderImpl.
                ? () -> smsSender.send(recipient, request.rawOtp())
                : () -> emailSender.send(recipient, notificationProperties.getEmail().getOtpSubject(), body);

        dispatch(correlationId, recipient, channel, NotificationType.OTP, send);
    }

    /**
     * Sends, then records the outcome. The failure row is delegated to
     * {@link NotificationFailureRecorder} - a separate bean, deliberately, so its
     * {@code REQUIRES_NEW} transaction is actually honoured; see that class for why a method on
     * this one would not be.
     */
    private void dispatch(
            String correlationId,
            String recipient,
            NotificationChannel channel,
            NotificationType type,
            Runnable send) {
        try {
            send.run();
        } catch (RuntimeException e) {
            log.warn(
                    "DISPATCH_FAILED type={} channel={} recipient={} correlationId={} cause={}",
                    type, channel, recipient, correlationId, e.toString());
            notificationFailureRecorder.recordFailure(recipient, channel, type, correlationId);
            throw e;
        }
        notificationRepository.save(
                new NotificationEntity(recipient, channel, type, NotificationStatus.SENT, correlationId));
    }

    private String resolveRecipient(NotificationChannel channel, OtpNotificationRequest request) {
        String recipient = channel == NotificationChannel.SMS ? request.recipientMobileNumber() : request.recipientEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipientMobileNumber/recipientEmail must match the declared channel " + channel);
        }
        return recipient;
    }
}
