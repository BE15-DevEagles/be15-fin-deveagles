package com.deveagles.be15_deveagles_be.features.notifications.command.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNoticeRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNotificationRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import com.deveagles.be15_deveagles_be.features.notifications.command.domain.repository.NotificationRepository;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

  @InjectMocks private NotificationCommandService notificationCommandService;

  @Mock private NotificationRepository notificationRepository;

  @Mock private NotificationSseService notificationSseService;

  @Test
  @DisplayName("알림 생성 성공 테스트")
  void create_notification_success() {
    // given
    final Long shopId = 1L;
    final String title = "테스트 제목";
    final String content = "테스트 내용입니다.";
    final NotificationType type = NotificationType.RESERVATION;

    CreateNotificationRequest request = new CreateNotificationRequest(shopId, type, title, content);
    Notification savedNotification =
        Notification.builder().shopId(shopId).title(title).content(content).type(type).build();

    given(notificationRepository.saveAndFlush(any(Notification.class)))
        .willReturn(savedNotification);

    // when
    NotificationResponse response = notificationCommandService.create(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo(title);
    assertThat(response.getContent()).isEqualTo(content);
    assertThat(response.getType()).isEqualTo(type);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).saveAndFlush(captor.capture());
    Notification capturedNotification = captor.getValue();

    assertThat(capturedNotification.getShopId()).isEqualTo(shopId);
    assertThat(capturedNotification.getTitle()).isEqualTo(title);
  }

  @Test
  @DisplayName("공지 생성 및 알림 발송 성공 테스트")
  void createNoticeAndNotify_success() {
    // given
    CreateNoticeRequest request = new CreateNoticeRequest(1L, "공지 제목", "공지 내용");
    Notification savedNotification =
        Notification.builder()
            .shopId(request.shopId())
            .title(request.title())
            .content(request.content())
            .type(NotificationType.NOTICE)
            .build();
    given(notificationRepository.saveAndFlush(any(Notification.class)))
        .willReturn(savedNotification);
    doNothing().when(notificationSseService).send(any(Long.class), any(NotificationResponse.class));

    // when
    notificationCommandService.createNoticeAndNotify(request);

    // then
    verify(notificationRepository).saveAndFlush(any(Notification.class));
    verify(notificationSseService).send(any(Long.class), any(NotificationResponse.class));
  }

  @Test
  @DisplayName("알림을 읽음으로 처리 성공 테스트")
  void markAsRead_success() {
    // given
    Long shopId = 1L;
    Long notificationId = 10L;
    Notification notification = Notification.builder().build(); // isRead는 기본 false

    given(notificationRepository.findByNotificationIdAndShopId(notificationId, shopId))
        .willReturn(Optional.of(notification));

    // when
    notificationCommandService.markAsRead(shopId, notificationId);

    // then
    assertThat(notification.isRead()).isTrue();
  }

  @Test
  @DisplayName("알림을 찾지 못했을 때 예외 발생 테스트")
  void markAsRead_fail_when_not_found() {
    // given
    Long shopId = 1L;
    Long notificationId = 10L;
    given(notificationRepository.findByNotificationIdAndShopId(notificationId, shopId))
        .willReturn(Optional.empty());

    // when & then
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              notificationCommandService.markAsRead(shopId, notificationId);
            });

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
  }
}
