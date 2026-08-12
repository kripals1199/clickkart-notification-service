// src/test/java/com/clickkart/notification/serviceImpl/NotificationDispatchServiceImplTest.java
package com.clickkart.notification.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.clickkart.notification.dto.request.OtpNotificationRequest;
import com.clickkart.notification.dto.request.PasswordResetNotificationRequest;
import com.clickkart.notification.entity.NotificationEntity;
import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;
import com.clickkart.notification.repository.NotificationRepository;
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

    private NotificationDispatchServiceImpl dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new NotificationDispatchServiceImpl(notificationRepository);
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
}
