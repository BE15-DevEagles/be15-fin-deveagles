package com.deveagles.be15_deveagles_be.features.notifications.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {}
