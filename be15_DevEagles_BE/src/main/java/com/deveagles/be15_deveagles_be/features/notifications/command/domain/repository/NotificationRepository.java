package com.deveagles.be15_deveagles_be.features.notifications.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  Optional<Notification> findByNotificationIdAndShopId(Long notificationId, Long shopId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.shopId = :shopId AND n.isRead = false")
  void markAllAsReadByShopId(@Param("shopId") Long shopId);
}
