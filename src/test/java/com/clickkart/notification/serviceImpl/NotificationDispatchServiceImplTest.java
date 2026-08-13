// src/test/java/com/clickkart/notification/serviceImpl/NotificationDispatchServiceImplTest.java
package com.clickkart.notification.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;
import com.clickkart.notification.entity.NotificationEntity;
import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;
import com.clickkart.notification.config.NotificationProperties;
import com.clickkart.notification.repository.NotificationRepository;
import com.clickkart.notification.service.EmailSender;
import com.clickkart.notification.service.SmsSender;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private SmsSender smsSender;

    @Mock
    private NotificationFailureRecorder notificationFailureRecorder;

    private NotificationDispatchServiceImpl dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new NotificationDispatchServiceImpl(
                notificationRepository,
                emailSender,
                smsSender,
                new NotificationProperties(),
                notificationFailureRecorder);
    }

    @Test
    void dispatchPasswordResetPersistsSentRecordWithoutRawToken() {
        PasswordResetNotificationRequest request =
                new PasswordResetNotificationRequest("user@example.com", "raw-reset-token-value", Instant.now().plusSeconds(1800));

        dispatchService.dispatchPasswordReset("correlation-id-1", request);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo("user@example.com");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getCorrelationId()).isEqualTo("correlation-id-1");
    }

    @Test
    void dispatchOtpResolvesRecipientFromMobileNumberForSmsChannel() {
        OtpNotificationRequest request = new OtpNotificationRequest(
                NotificationChannel.SMS, null, "9845550100", "042817", Instant.now().plusSeconds(300));

        dispatchService.dispatchOtp("correlation-id-1", request);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo("9845550100");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.OTP);
    }

    @Test
    void dispatchOtpResolvesRecipientFromEmailForEmailChannel() {
        OtpNotificationRequest request = new OtpNotificationRequest(
                NotificationChannel.EMAIL, "user@example.com", null, "042817", Instant.now().plusSeconds(300));

        dispatchService.dispatchOtp("correlation-id-1", request);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo("user@example.com");
    }

    @Test
    void dispatchOtpRejectsMissingRecipientForDeclaredChannel() {
        OtpNotificationRequest request = new OtpNotificationRequest(
                NotificationChannel.SMS, null, null, "042817", Instant.now().plusSeconds(300));

        assertThrows(IllegalArgumentException.class, () -> dispatchService.dispatchOtp("correlation-id-1", request));

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void dispatchPasswordResetActuallyHandsTheMessageToTheEmailSender() {
        PasswordResetNotificationRequest request = new PasswordResetNotificationRequest(
                "user@example.com", "raw-reset-token-value", Instant.now().plusSeconds(1800));

        dispatchService.dispatchPasswordReset("correlation-id-1", request);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("user@example.com"), any(), body.capture());
        // The raw token must reach the recipient - it exists nowhere else, since only its hash
        // is persisted by Auth Service.
        assertThat(body.getValue()).contains("raw-reset-token-value");
        verify(smsSender, org.mockito.Mockito.never()).send(any(), any());
    }

    @Test
    void dispatchOtpOverSmsUsesTheSmsSenderAndPassesOnlyTheCode() {
        OtpNotificationRequest request = new OtpNotificationRequest(
                NotificationChannel.SMS, null, "9845550100", "042817", Instant.now().plusSeconds(300));

        dispatchService.dispatchOtp("correlation-id-1", request);

        // Only the bare code - MSG91's DLT-registered template supplies the wording.
        verify(smsSender).send("9845550100", "042817");
        verify(emailSender, org.mockito.Mockito.never()).send(any(), any(), any());
    }

    @Test
    void aFailedSendIsRecordedAsFailedAndRethrown() {
        PasswordResetNotificationRequest request = new PasswordResetNotificationRequest(
                "user@example.com", "raw-reset-token-value", Instant.now().plusSeconds(1800));
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(emailSender)
                .send(any(), any(), any());

        // Must propagate: Auth Service turns this into a 503 rather than telling the user a reset
        // link is on its way when nothing was sent.
        assertThrows(org.springframework.mail.MailSendException.class,
                () -> dispatchService.dispatchPasswordReset("correlation-id-1", request));

        verify(notificationFailureRecorder).recordFailure(
                "user@example.com", NotificationChannel.EMAIL, NotificationType.PASSWORD_RESET, "correlation-id-1");
        // and no SENT row was written
        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }
}
