// src/main/java/com/clickkart/notification/entity/NotificationEntity.java
package com.clickkart.notification.entity;

import com.clickkart.notification.enums.NotificationChannel;
import com.clickkart.notification.enums.NotificationStatus;
import com.clickkart.notification.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Durable record that a dispatch was attempted - deliberately does NOT store the raw
 * token/OTP/verification-code value itself (that only ever exists transiently, in the simulated
 * dispatch log line - see {@code NotificationDispatchServiceImpl}), so this table's blast radius
 * on compromise doesn't include every secret this service has ever "sent".
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notifications",
        indexes = {
            @Index(name = "idx_notifications_correlation_id", columnList = "correlation_id"),
            @Index(name = "idx_notifications_created_date", columnList = "created_date")
        })
public class NotificationEntity extends BaseEntity {

    @Column(name = "recipient", nullable = false, length = 254)
    private String recipient;

    // JdbcTypeCode(VARCHAR) alongside @Enumerated(STRING) prevents Hibernate from
    // auto-generating a CHECK constraint frozen at whatever this enum's values are at
    // table-creation time - see clickkart-auth-service's AuditLogEntryEntity for the full
    // rationale (ddl-auto=update never widens an existing constraint, no migration tool here).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationStatus status;

    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    public NotificationEntity(
            String recipient,
            NotificationChannel channel,
            NotificationType notificationType,
            NotificationStatus status,
            String correlationId) {
        this.recipient = recipient;
        this.channel = channel;
        this.notificationType = notificationType;
        this.status = status;
        this.correlationId = correlationId;
    }
}
