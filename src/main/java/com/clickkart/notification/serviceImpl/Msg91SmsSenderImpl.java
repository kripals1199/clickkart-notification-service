// src/main/java/com/clickkart/notification/serviceImpl/Msg91SmsSenderImpl.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.config.NotificationProperties;
import com.clickkart.notification.constant.LoggerNames;
import com.clickkart.notification.service.SmsSender;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.clickkart.notification.config.ProviderConfiguredConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Real SMS delivery via MSG91's v5 flow API. Active only when
 * {@code clickkart.notification.sms.msg91.auth-key} is set; otherwise {@code LoggingSmsSenderImpl}
 * takes over.
 *
 * <p><b>Indian transactional SMS is template-bound by law.</b> Under TRAI's DLT regime the
 * message body cannot be free-form - it must match a template already registered and approved
 * against the sender id. That is why this sends a {@code template_id} plus a variable map rather
 * than raw text: the message the user receives is assembled by MSG91 from the approved template,
 * and the {@code otp} variable below must match the variable name used when registering it.
 * A mismatched or unapproved template is rejected by the carrier, not by this code.
 *
 * <p>Failures propagate. {@code NotificationDispatchServiceImpl} records the attempt as
 * {@code FAILED} and lets the error reach Auth Service, so an OTP request never reports success
 * when no message was sent.
 */
@Slf4j(topic = LoggerNames.DISPATCH)
@Component
@Conditional(ProviderConfiguredConditions.Msg91Configured.class)
public class Msg91SmsSenderImpl implements SmsSender {

    private static final String FLOW_PATH = "/api/v5/flow/";
    private static final String AUTH_HEADER = "authkey";

    private final NotificationProperties.Sms.Msg91 config;
    private final RestClient restClient;

    // Builds its own RestClient rather than injecting RestClient.Builder: that builder is not an
    // auto-configured bean in this application's context, so injecting it failed startup outright.
    // Nothing here needs the shared builder's customizers - this client talks to exactly one
    // external API with its own base URL and auth header.
    public Msg91SmsSenderImpl(NotificationProperties notificationProperties) {
        this.config = notificationProperties.getSms().getMsg91();
        this.restClient = RestClient.create(config.getBaseUrl());
    }

    @Override
    public void send(String toMobileNumber, String message) {
        // MSG91 expects the number country-code prefixed and digits-only.
        String recipient = config.getCountryCode() + toMobileNumber.replaceAll("\\D", "");

        Map<String, Object> recipientEntry = new LinkedHashMap<>();
        recipientEntry.put("mobiles", recipient);
        // Variable name must match the DLT-registered template's placeholder.
        recipientEntry.put("otp", message);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("template_id", config.getTemplateId());
        if (config.getSenderId() != null && !config.getSenderId().isBlank()) {
            payload.put("sender", config.getSenderId());
        }
        payload.put("recipients", List.of(recipientEntry));

        restClient
                .post()
                .uri(FLOW_PATH)
                .header(AUTH_HEADER, config.getAuthKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        // Recipient only - never the message body, which carries the OTP.
        log.info("SMS_SENT recipient={} templateId={}", recipient, config.getTemplateId());
    }

    @Override
    public boolean isRealDelivery() {
        return true;
    }
}
