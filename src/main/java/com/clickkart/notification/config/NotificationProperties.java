// src/main/java/com/clickkart/notification/config/NotificationProperties.java
package com.clickkart.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "clickkart.notification")
public class NotificationProperties {

    private final Email email = new Email();
    private final Sms sms = new Sms();

    @Getter
    @Setter
    public static class Email {
        /**
         * Envelope From address. With Gmail SMTP this MUST be the same account as
         * spring.mail.username - Gmail rewrites or rejects a mismatched From, so a "spoofed"
         * sender silently fails to arrive.
         */
        private String from;

        private String passwordResetSubject = "Reset your ClickKart password";
        private String otpSubject = "Your ClickKart verification code";
    }

    @Getter
    @Setter
    public static class Sms {
        private final Msg91 msg91 = new Msg91();

        @Getter
        @Setter
        public static class Msg91 {
            /** Presence of this key is what activates real SMS delivery - see SmsSender. */
            private String authKey;

            /**
             * DLT-registered flow/template id. Indian transactional SMS legally requires the
             * message to match a pre-registered template, so this cannot be a free-form string
             * and the template must already be approved in the MSG91 console.
             */
            private String templateId;

            private String senderId;
            private String baseUrl = "https://control.msg91.com";
            /** Country code prefixed to the 10-digit number MSG91 expects. */
            private String countryCode = "91";
        }
    }
}
