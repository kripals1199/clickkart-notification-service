// src/main/java/com/clickkart/notification/entity/BaseEntity.java
package com.clickkart.notification.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;

/** {@code id} is strictly framework-managed - never set directly by application code. */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq_gen")
    @SequenceGenerator(name = "notification_seq_gen", sequenceName = "notification_seq", allocationSize = 1)
    private Long id;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @PrePersist
    void onCreate() {
        this.createdDate = Instant.now();
    }
}
