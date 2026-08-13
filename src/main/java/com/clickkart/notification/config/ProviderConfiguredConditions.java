// src/main/java/com/clickkart/notification/config/ProviderConfiguredConditions.java
package com.clickkart.notification.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Conditions that treat a <b>blank</b> property as "not configured".
 *
 * <p>{@code @ConditionalOnProperty(name = "...")} is not usable here: it matches whenever the
 * property is <i>present</i> and not literally {@code false}, and an empty string counts as
 * present. Every environment declares these keys with an empty default (e.g.
 * {@code ${MSG91_AUTH_KEY:}}) so the property always exists - meaning the real senders activated
 * even with no credentials at all. That surfaced as an application that failed to start rather
 * than one that quietly tried to send with an empty auth key, but either outcome is wrong.
 *
 * <p>These conditions check for actual content instead, so "unset" and "set to empty" behave
 * identically and the logging fallbacks take over as intended.
 */
public final class ProviderConfiguredConditions {

    private ProviderConfiguredConditions() {}

    private static boolean hasText(ConditionContext context, String property) {
        String value = context.getEnvironment().getProperty(property);
        return value != null && !value.isBlank();
    }

    /** Real SMTP delivery is possible only when a mail host is actually set. */
    public static class SmtpConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return hasText(context, "spring.mail.host");
        }
    }

    /** Real MSG91 delivery is possible only when an auth key is actually set. */
    public static class Msg91Configured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return hasText(context, "clickkart.notification.sms.msg91.auth-key");
        }
    }
}
