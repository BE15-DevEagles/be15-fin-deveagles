package com.deveagles.be15_deveagles_be.features.notifications.query.presentation;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 조회 API", description = "알림 목록 및 개수 조회 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

  private final NotificationQueryService notificationQueryService;

  @Operation(summary = "알림 목록 조회", description = "특정 매장의 알림 목록을 최신순으로 페이징하여 조회합니다.")
  @GetMapping
  public ResponseEntity<Page<NotificationResponse>> getNotifications(
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam("shop-id")
          Long shopId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    Page<NotificationResponse> notifications =
        notificationQueryService.getNotificationsByShop(shopId, pageable);
    return ResponseEntity.ok(notifications);
  }

  @Operation(summary = "읽지 않은 알림 개수 조회", description = "특정 매장의 읽지 않은 알림 개수를 조회합니다.")
  @GetMapping("/unread-count")
  public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam("shop-id")
          Long shopId) {

    UnreadNotificationCountResponse response =
        notificationQueryService.getUnreadNotificationCount(shopId);
    return ResponseEntity.ok(response);
  }
}
