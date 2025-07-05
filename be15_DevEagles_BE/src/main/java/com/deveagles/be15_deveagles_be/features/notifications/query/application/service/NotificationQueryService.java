package com.deveagles.be15_deveagles_be.features.notifications.query.application.service;

import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.UnreadNotificationCountResponse;
import com.deveagles.be15_deveagles_be.features.notifications.query.repository.NotificationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이므로 readOnly=true로 성능 최적화
public class NotificationQueryService {

  private final NotificationQueryRepository notificationQueryRepository;

  /**
   * 특정 매장의 알림 목록을 조회합니다.
   *
   * @param shopId 조회할 매장의 ID
   * @param pageable 페이징 및 정렬 정보
   * @return 페이징된 알림 응답 DTO 목록
   */
  public Page<NotificationResponse> getNotificationsByShop(Long shopId, Pageable pageable) {
    return notificationQueryRepository.findByShopId(shopId, pageable);
  }

  /**
   * 특정 매장의 읽지 않은 알림 개수를 조회합니다.
   *
   * @param shopId 조회할 매장의 ID
   * @return 읽지 않은 알림 개수 응답 DTO
   */
  public UnreadNotificationCountResponse getUnreadNotificationCount(Long shopId) {
    long count = notificationQueryRepository.countByShopIdAndIsReadFalse(shopId);
    return new UnreadNotificationCountResponse(count);
  }
}
