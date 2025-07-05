package com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification") // 컨벤션 2.2: snake_case 테이블명 명시
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 컨벤션 2.8: 프록시 생성을 위한 protected 기본 생성자
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long notificationId; // DDL에 맞춰 필드명 수정

  @Column(name = "shop_id", nullable = false)
  private Long shopId;

  @Column(name = "title", length = 100)
  private String title;

  @Lob // DDL의 TEXT 타입 매핑
  @Column(name = "content")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private NotificationType type;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  public Notification(Long shopId, String title, String content, NotificationType type) {
    this.shopId = shopId;
    this.title = title;
    this.content = content;
    this.type = type;
  }
}
