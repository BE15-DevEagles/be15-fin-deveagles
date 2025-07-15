package com.deveagles.be15_deveagles_be.features.schedules.command.application.handler;

import com.deveagles.be15_deveagles_be.common.events.ReservationCreatedEvent;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNotificationRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.service.NotificationCommandService;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.service.NotificationSseService;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNotificationHandler {

  private final NotificationCommandService notificationCommandService;
  private final NotificationSseService notificationSseService;

  /**
   * `@Transactional`에는 새로운 트랜잭션을 시작하도록 `propagation = Propagation.REQUIRES_NEW` 옵션을 추가합니다. 이렇게 하면
   * 예약 트랜잭션과는 독립적으로 알림 트랜잭션이 관리됩니다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handle(ReservationCreatedEvent event) {
    log.info("[Event] ReservationCreatedEvent 수신 - shopId: {}", event.shopId());

    try {
      CreateNotificationRequest request =
          new CreateNotificationRequest(
              event.shopId(),
              NotificationType.RESERVATION,
              "새로운 예약 신청",
              String.format("고객 '%s'님이 예약을 신청했습니다. 확인해주세요.", event.customerName()));

      NotificationResponse savedNotification = notificationCommandService.create(request);

      // ID가 정상적으로 생성되었는지 확인 후 SSE 발송
      if (savedNotification != null && savedNotification.getNotificationId() != null) {
        notificationSseService.send(event.shopId(), savedNotification);
        log.info("[Notification] 예약 알림 생성 및 SSE 발송 성공");
      } else {
        log.warn("[Notification] 예약 알림이 생성되었으나 ID가 null입니다. DB 저장을 확인하세요.");
      }
    } catch (Exception e) {
      log.error("[Notification] 예약 알림 처리 중 예외 발생", e);
    }
  }
}
