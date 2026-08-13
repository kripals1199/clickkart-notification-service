// src/test/java/com/clickkart/notification/serviceImpl/SendersTest.java
package com.clickkart.notification.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.clickkart.notification.config.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SendersTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void smtpSenderBuildsTheMessageAndDelegatesToJavaMailSender() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setFrom("no-reply@clickkart.local");
        SmtpEmailSenderImpl sender = new SmtpEmailSenderImpl(javaMailSender, properties);

        sender.send("user@example.com", "Subject line", "Body with token abc123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getFrom()).isEqualTo("no-reply@clickkart.local");
        assertThat(sent.getSubject()).isEqualTo("Subject line");
        assertThat(sent.getText()).isEqualTo("Body with token abc123");
        assertThat(sender.isRealDelivery()).isTrue();
    }

    @Test
    void smtpSenderOmitsFromWhenNotConfiguredSoTheProviderDefaultApplies() {
        // Gmail rewrites a mismatched From anyway; leaving it unset lets the transport decide
        // rather than sending an address that would be silently replaced or rejected.
        SmtpEmailSenderImpl sender = new SmtpEmailSenderImpl(javaMailSender, new NotificationProperties());

        sender.send("user@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isNull();
    }

    @Test
    void smtpSenderLetsDeliveryFailuresPropagate() {
        // Must NOT be swallowed: the caller records FAILED and returns 503 rather than telling
        // the user a reset link is on its way when nothing was sent.
        SmtpEmailSenderImpl sender = new SmtpEmailSenderImpl(javaMailSender, new NotificationProperties());
        doThrow(new MailSendException("smtp refused")).when(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send("user@example.com", "s", "b"))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    void loggingEmailSenderReportsItselfAsNotRealDelivery() {
        LoggingEmailSenderImpl sender = new LoggingEmailSenderImpl();
        sender.warnNotRealDelivery();
        sender.send("user@example.com", "Subject", "Body");
        // The distinction callers rely on to know whether a message was genuinely delivered.
        assertThat(sender.isRealDelivery()).isFalse();
    }

    @Test
    void loggingSmsSenderReportsItselfAsNotRealDelivery() {
        LoggingSmsSenderImpl sender = new LoggingSmsSenderImpl();
        sender.warnNotRealDelivery();
        sender.send("9845550100", "042817");
        assertThat(sender.isRealDelivery()).isFalse();
    }

    @Test
    void msg91SenderReportsItselfAsRealDelivery() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().getMsg91().setAuthKey("key");
        properties.getSms().getMsg91().setTemplateId("template");
        Msg91SmsSenderImpl sender = new Msg91SmsSenderImpl(properties);

        assertThat(sender.isRealDelivery()).isTrue();
    }
}
