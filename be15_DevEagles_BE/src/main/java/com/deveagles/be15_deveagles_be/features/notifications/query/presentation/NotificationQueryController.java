package com.deveagles.be15_deveagles_be.features.notifications.query.presentation;

import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.UnreadNotificationCountResponse;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 조회 API", description = "알림 목록 및 개수 조회 관련 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

  private final NotificationQueryService notificationQueryService;

  @Operation(summary = "알림 목록 조회", description = "특정 매장의 알림 목록을 최신순으로 페이징하여 조회합니다.")
  @GetMapping
  public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUser customUser,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    Page<NotificationResponse> notifications =
        notificationQueryService.getNotificationsByShop(customUser.getShopId(), pageable);
    return ResponseEntity.ok(notifications);
  }

  @Operation(summary = "내 읽지 않은 알림 개수 조회", description = "로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
  @GetMapping("/unread-count")
  public ResponseEntity<UnreadNotificationCountResponse> getMyUnreadCount(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUser customUser) {

    UnreadNotificationCountResponse response =
        notificationQueryService.getUnreadNotificationCount(customUser.getShopId());
    return ResponseEntity.ok(response);
  }
}
