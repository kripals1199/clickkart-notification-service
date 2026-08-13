// src/main/java/com/clickkart/notification/serviceImpl/LoggingEmailSender.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.service.EmailSender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback used when no SMTP host is configured: writes the full message to the DISPATCH log
 * instead of sending it. This keeps the platform runnable end-to-end in dev without credentials
 * - a developer reads the reset token or OTP out of {@code logs/dispatch.log}.
 *
 * <p>Logs a startup WARN precisely because this mode is easy to leave switched on by accident.
 * In any environment where a user is expected to actually receive the message, this bean being
 * active is a misconfiguration, not a fallback.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Component
@ConditionalOnMissingBean(SmtpEmailSender.class)
public class LoggingEmailSender implements EmailSender {

    @PostConstruct
    void warnNotRealDelivery() {
        log.warn(
                "EMAIL_DELIVERY_SIMULATED - spring.mail.host is not configured, so NO email is "
                        + "actually sent. Message bodies (including raw reset tokens and OTPs) are written "
                        + "to this log instead. Intended for local development only.");
    }

    @Override
    public void send(String to, String subject, String body) {
        log.info("SIMULATED_EMAIL recipient={} subject=\"{}\" body=\"{}\"", to, subject, body);
    }

    @Override
    public boolean isRealDelivery() {
        return false;
    }
}
