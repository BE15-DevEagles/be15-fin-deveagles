package com.deveagles.be15_deveagles_be.features.notifications.query.repository;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.Notification;
import com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationQueryRepository extends JpaRepository<Notification, Long> {

  /**
   * 특정 매장(shopId)의 알림 목록을 페이징하여 조회합니다. DTO 생성자 표현식을 사용하여 필요한 데이터만 조회합니다.
   *
   * @param shopId 조회할 매장의 ID
   * @param pageable 페이징 및 정렬 정보
   * @return 페이징된 알림 응답 DTO 목록
   */
  @Query(
      "SELECT new com.deveagles.be15_deveagles_be.features.notifications.query.application.dto.NotificationResponse(n.notificationId, n.title, n.content, n.type, n.isRead, n.createdAt) "
          + "FROM Notification n WHERE n.shopId = :shopId")
  Page<NotificationResponse> findByShopId(@Param("shopId") Long shopId, Pageable pageable);

  /**
   * 특정 매장의 읽지 않은 알림 개수를 조회합니다.
   *
   * @param shopId 조회할 매장의 ID
   * @return 읽지 않은 알림의 수
   */
  long countByShopIdAndIsReadFalse(Long shopId);
}
