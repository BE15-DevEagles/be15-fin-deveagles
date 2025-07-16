package com.deveagles.be15_deveagles_be.features.notifications.command.application.controller;

import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.dto.CreateNoticeRequest;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.service.NotificationCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림 변경 API", description = "알림 상태 변경 관련 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationCommandController {

  private final NotificationCommandService notificationCommandService;

  @Operation(summary = "공지사항 알림 생성 및 발송", description = "관리자가 특정 매장에 공지사항 알림을 생성하고 실시간으로 발송합니다.")
  @PostMapping("/notices")
  public ResponseEntity<Void> createNotice(@Valid @RequestBody CreateNoticeRequest request) {
    notificationCommandService.createNoticeAndNotify(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
  @PatchMapping("/{notificationId}/read") // 예: PATCH /api/v1/notifications/101/read
  public ResponseEntity<Void> markNotificationAsRead(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long notificationId) {

    notificationCommandService.markAsRead(customUser.getShopId(), notificationId);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "모든 알림 읽음 처리", description = "현재 로그인한 사용자의 모든 알림을 읽음 상태로 일괄 변경합니다.")
  @PutMapping("/read-all")
  public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    notificationCommandService.markAllAsRead(shopId);
    return ResponseEntity.noContent().build();
  }
}
