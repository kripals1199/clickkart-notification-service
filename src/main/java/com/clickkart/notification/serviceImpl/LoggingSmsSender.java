// src/main/java/com/clickkart/notification/serviceImpl/LoggingSmsSender.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.service.SmsSender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback used when no MSG91 auth key is configured: writes the message to the DISPATCH log
 * instead of sending it. Keeps the platform runnable without a paid SMS account.
 *
 * <p>Logs a startup WARN for the same reason as {@code LoggingEmailSender} - in any environment
 * where a user is expected to actually receive an SMS, this bean being active is a
 * misconfiguration rather than a fallback.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Component
@ConditionalOnMissingBean(Msg91SmsSender.class)
public class LoggingSmsSender implements SmsSender {

    @PostConstruct
    void warnNotRealDelivery() {
        log.warn(
                "SMS_DELIVERY_SIMULATED - clickkart.notification.sms.msg91.auth-key is not configured, "
                        + "so NO SMS is actually sent. Message bodies (including raw OTPs) are written to this "
                        + "log instead. Intended for local development only.");
    }

    @Override
    public void send(String toMobileNumber, String message) {
        log.info("SIMULATED_SMS recipient={} message=\"{}\"", toMobileNumber, message);
    }

    @Override
    public boolean isRealDelivery() {
        return false;
    }
}
