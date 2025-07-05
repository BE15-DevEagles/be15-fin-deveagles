package com.deveagles.be15_deveagles_be.features.notifications.command.application.service;

import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNotificationRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

  private final NotificationRepository notificationRepository;

  public void create(CreateNotificationRequest request) {
    Notification notification =
        Notification.builder()
            .shopId(request.getShopId())
            .type(request.getType())
            .title(request.getTitle())
            .content(request.getContent())
            .build();
    notificationRepository.save(notification);
  }
}
