package com.deveagles.be15_deveagles_be.features.notifications.command.application.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNoticeRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNotificationRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.repository.NotificationRepository;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

  private final NotificationRepository notificationRepository;
  private final NotificationSseService notificationSseService;

  /**
   * 알림을 생성하고, 생성된 알림 정보를 DTO로 반환합니다.
   *
   * @param request 알림 생성에 필요한 정보
   * @return 생성된 알림의 상세 정보가 담긴 DTO
   */
  public NotificationResponse create(CreateNotificationRequest request) {
    Notification notification =
        Notification.builder()
            .shopId(request.getShopId())
            .type(request.getType())
            .title(request.getTitle())
            .content(request.getContent())
            .build();

    Notification savedNotification = notificationRepository.saveAndFlush(notification);

    return new NotificationResponse(
        savedNotification.getNotificationId(),
        savedNotification.getTitle(),
        savedNotification.getContent(),
        savedNotification.getType(),
        savedNotification.isRead(),
        savedNotification.getCreatedAt());
  }

  public void createNoticeAndNotify(CreateNoticeRequest request) {
    CreateNotificationRequest notificationRequest =
        new CreateNotificationRequest(
            request.shopId(), NotificationType.NOTICE, request.title(), request.content());
    NotificationResponse savedNotification = this.create(notificationRequest);

    notificationSseService.send(request.shopId(), savedNotification);
  }

  public void markAsRead(Long shopId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByNotificationIdAndShopId(notificationId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

    notification.markAsRead();
  }
}
