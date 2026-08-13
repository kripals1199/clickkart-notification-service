// src/main/java/com/clickkart/notification/serviceImpl/NotificationFailureRecorder.java
package com.clickkart.notification.serviceImpl;

import com.clickkart.notification.entity.NotificationEntity;
import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;
import com.clickkart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the {@code FAILED} audit row for a dispatch that threw, in its own transaction.
 *
 * <p>This exists as a <b>separate bean</b> rather than a method on
 * {@code NotificationDispatchServiceImpl} for a reason that is easy to get wrong: Spring's
 * transaction support is proxy-based, so {@code @Transactional} is only honoured on calls that
 * arrive through the proxy. A self-invocation inside the same class bypasses the proxy entirely
 * and the {@code REQUIRES_NEW} would be silently ignored - the row would join the caller's
 * transaction and be rolled back by the very exception it is meant to record, leaving a failed
 * send with no trace at all.
 *
 * <p>Same pattern, and same reasoning, as {@code AuthFailureRecorder} in clickkart-auth-service.
 */
@Component
@RequiredArgsConstructor
public class NotificationFailureRecorder {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(
            String recipient, NotificationChannel channel, NotificationType type, String correlationId) {
        notificationRepository.save(
                new NotificationEntity(recipient, channel, type, NotificationStatus.FAILED, correlationId));
    }
}
