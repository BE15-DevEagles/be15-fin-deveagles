package com.deveagles.be15_deveagles_be.features.notifications.query.application.dto;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Schema(description = "알림 단건 조회 응답 DTO")
public class NotificationResponse {

  @Schema(description = "알림 고유 ID", example = "101")
  private final Long notificationId;

  @Schema(description = "알림 제목", example = "새로운 예약 신청")
  private final String title;

  @Schema(description = "알림 내용", example = "고객 '박포스트맨'님이 예약을 신청했습니다. 확인해주세요.")
  private final String content;

  @Schema(description = "알림 타입 (NOTICE, RESERVATION, ANALYSIS)", example = "RESERVATION")
  private final NotificationType type;

  @Schema(description = "읽음 여부", example = "false")
  private final boolean isRead;

  @Schema(description = "알림 생성 시각", example = "2025-07-29T10:00:00")
  private final LocalDateTime createdAt;

  public NotificationResponse(
      Long notificationId,
      String title,
      String content,
      NotificationType type,
      boolean isRead,
      LocalDateTime createdAt) {
    this.notificationId = notificationId;
    this.title = title;
    this.content = content;
    this.type = type;
    this.isRead = isRead;
    this.createdAt = createdAt;
  }
}
