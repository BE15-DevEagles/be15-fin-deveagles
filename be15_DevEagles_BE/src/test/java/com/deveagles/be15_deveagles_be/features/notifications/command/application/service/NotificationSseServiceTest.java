package com.deveagles.be15_deveagles_be.features.notifications.command.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NotificationSseServiceTest {

  @InjectMocks private NotificationSseService notificationSseService;

  @BeforeEach
  void setUp() {
    notificationSseService = new NotificationSseService();
  }

  @Test
  @DisplayName("SSE 구독 시 SseEmitter가 정상적으로 반환된다")
  void subscribe_success() {
    // given
    final Long shopId = 1L;

    // when
    SseEmitter emitter = notificationSseService.subscribe(shopId);

    // then
    assertThat(emitter).isNotNull();
  }

  @Test
  @DisplayName("구독한 사용자에게 알림 발송 시 오류가 발생하지 않는다")
  void send_notification_to_subscribed_user() {
    // given
    final Long shopId = 1L;
    notificationSseService.subscribe(shopId); // 1번 사용자를 구독 상태로 만듦

    NotificationResponse notification =
        new NotificationResponse(
            101L, "테스트 알림", "내용입니다.", NotificationType.RESERVATION, false, LocalDateTime.now());

    // when & then
    assertDoesNotThrow(() -> notificationSseService.send(shopId, notification));
  }

  @Test
  @DisplayName("구독하지 않은 사용자에게는 알림 발송 로직이 실행되지 않는다")
  void send_notification_to_unsubscribed_user() {
    // given
    final Long subscribedShopId = 1L;
    final Long unsubscribedShopId = 2L;

    notificationSseService.subscribe(subscribedShopId);

    NotificationResponse notification =
        new NotificationResponse(
            102L,
            "테스트 알림",
            "구독 안한 사람에게 가는 내용",
            NotificationType.NOTICE,
            false,
            LocalDateTime.now());

    // when & then
    assertDoesNotThrow(() -> notificationSseService.send(unsubscribedShopId, notification));
  }
}
