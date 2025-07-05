package com.deveagles.be15_deveagles_be.features.notifications.query.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "읽지 않은 알림 개수 조회 응답 DTO")
public class UnreadNotificationCountResponse {

  @Schema(description = "읽지 않은 알림의 총 개수", example = "5")
  private final long unreadCount;
}
