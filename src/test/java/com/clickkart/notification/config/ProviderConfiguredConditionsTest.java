// src/test/java/com/clickkart/notification/config/ProviderConfiguredConditionsTest.java
package com.clickkart.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

/**
 * These conditions exist because {@code @ConditionalOnProperty} got this wrong in a way that
 * broke the application at startup: every environment declares the provider keys with an empty
 * default (e.g. {@code ${MSG91_AUTH_KEY:}}), so the property is always *present* and the real
 * sender activated with no credentials at all.
 *
 * <p>The empty-string cases below are therefore the whole point of these tests, not edge-case
 * padding.
 */
class ProviderConfiguredConditionsTest {

    private ConditionContext contextWith(String property, String value) {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(property)).thenReturn(value);
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return context;
    }

    @Test
    void smtpIsConfiguredWhenHostHasAValue() {
        var condition = new ProviderConfiguredConditions.SmtpConfigured();
        assertThat(condition.matches(contextWith("spring.mail.host", "smtp.gmail.com"), null)).isTrue();
    }

    @Test
    void smtpIsNotConfiguredWhenHostIsEmpty() {
        // The regression case: property present but blank must count as "not configured".
        var condition = new ProviderConfiguredConditions.SmtpConfigured();
        assertThat(condition.matches(contextWith("spring.mail.host", ""), null)).isFalse();
    }

    @Test
    void smtpIsNotConfiguredWhenHostIsWhitespaceOnly() {
        var condition = new ProviderConfiguredConditions.SmtpConfigured();
        assertThat(condition.matches(contextWith("spring.mail.host", "   "), null)).isFalse();
    }

    @Test
    void smtpIsNotConfiguredWhenHostIsAbsent() {
        var condition = new ProviderConfiguredConditions.SmtpConfigured();
        assertThat(condition.matches(contextWith("spring.mail.host", null), null)).isFalse();
    }

    @Test
    void msg91IsConfiguredWhenAuthKeyHasAValue() {
        var condition = new ProviderConfiguredConditions.Msg91Configured();
        assertThat(condition.matches(
                contextWith("clickkart.notification.sms.msg91.auth-key", "real-key"), null)).isTrue();
    }

    @Test
    void msg91IsNotConfiguredWhenAuthKeyIsEmpty() {
        // This exact case previously activated Msg91SmsSenderImpl and failed application startup.
        var condition = new ProviderConfiguredConditions.Msg91Configured();
        assertThat(condition.matches(
                contextWith("clickkart.notification.sms.msg91.auth-key", ""), null)).isFalse();
    }

    @Test
    void msg91IsNotConfiguredWhenAuthKeyIsAbsent() {
        var condition = new ProviderConfiguredConditions.Msg91Configured();
        assertThat(condition.matches(
                contextWith("clickkart.notification.sms.msg91.auth-key", null), null)).isFalse();
    }
}
