// src/main/java/com/clickkart/notification/repository/NotificationRepository.java
package com.clickkart.notification.repository;

import com.clickkart.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {}
