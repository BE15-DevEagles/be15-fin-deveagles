package com.deveagles.be15_deveagles_be.features.schedules.command.application.handler;

import com.deveagles.be15_deveagles_be.common.events.ReservationCreatedEvent;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNotificationRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.service.NotificationCommandService;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNotificationHandler {

  // 중앙 알림 서비스를 주입받아 사용합니다.
  private final NotificationCommandService notificationCommandService;

  // ReservationCreatedEvent가 발행되면 이 메서드가 비동기적으로 실행됩니다.
  @Async
  @EventListener
  public void handle(ReservationCreatedEvent event) {
    log.info("[Event] ReservationCreatedEvent 수신 - shopId: {}", event.shopId());

    // 알림 생성에 필요한 데이터를 가공합니다.
    CreateNotificationRequest request =
        new CreateNotificationRequest(
            event.shopId(),
            NotificationType.RESERVATION,
            "새로운 예약 신청",
            String.format("고객 '%s'님이 예약을 신청했습니다. 확인해주세요.", event.customerName()));

    // 중앙 알림 서비스에 알림 생성을 요청합니다.
    notificationCommandService.create(request);
  }
}
