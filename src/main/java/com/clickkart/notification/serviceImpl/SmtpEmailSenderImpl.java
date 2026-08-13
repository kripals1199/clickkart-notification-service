// src/main/java/com/clickkart/notification/serviceImpl/SmtpEmailSenderImpl.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.config.NotificationProperties;
import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.clickkart.notification.config.ProviderConfiguredConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email delivery. Active only when {@code spring.mail.host} is set - otherwise
 * {@code LoggingEmailSenderImpl} takes over and the service runs without credentials.
 *
 * <p>Deliberately does NOT catch {@code MailException}: a failed send must reach
 * {@code NotificationDispatchServiceImpl} so the attempt is recorded as {@code FAILED} and the
 * error propagates to Auth Service. Swallowing it would let a password-reset request report
 * success while the user never receives the token - the exact failure mode this service exists
 * to prevent.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Component
@RequiredArgsConstructor
@Conditional(ProviderConfiguredConditions.SmtpConfigured.class)
public class SmtpEmailSenderImpl implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final NotificationProperties notificationProperties;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        String from = notificationProperties.getEmail().getFrom();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);

        // Subject only - never the body, which carries the raw reset token or OTP. The whole
        // point of hashing those before persistence is defeated if they land in a log file.
        log.info("EMAIL_SENT recipient={} subject=\"{}\"", to, subject);
    }

    @Override
    public boolean isRealDelivery() {
        return true;
    }
}
