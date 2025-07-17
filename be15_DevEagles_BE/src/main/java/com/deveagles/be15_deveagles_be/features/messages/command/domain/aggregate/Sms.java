package com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Sms {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "message_content", nullable = false, length = 500)
  private String messageContent;

  @Column(name = "sent_at", nullable = true)
  private LocalDateTime sentAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_delivery_status", nullable = false)
  private MessageDeliveryStatus messageDeliveryStatus;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false)
  private MessageType messageType;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_sending_type", nullable = false)
  private MessageSendingType messageSendingType;

  @Column(name = "template_id")
  private Long templateId;

  @Column(name = "shop_id", nullable = false)
  private Long shopId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Builder.Default
  @Column(name = "has_link", nullable = false)
  private Boolean hasLink = false;

  @Column(name = "customer_grade_id")
  private Long customerGradeId;

  @Column(name = "tag_id")
  private Long tagId;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_kind", nullable = false)
  private MessageKind messageKind;

  @Column(name = "coupon_id", nullable = true)
  private Long couponId;

  @Column(name = "workflow_id", nullable = true)
  private Long workflowId;

  public void markAsSent() {
    this.sentAt = LocalDateTime.now();
    this.messageDeliveryStatus = MessageDeliveryStatus.SENT;
  }

  public void markAsFailed() {
    this.messageDeliveryStatus = MessageDeliveryStatus.FAIL;
  }

  public void cancel() {
    this.messageDeliveryStatus = MessageDeliveryStatus.CANCELLED;
  }

  public void updateContent(String newContent) {
    this.messageContent = newContent;
  }

  public void schedule(LocalDateTime time) {
    if (this.messageSendingType != MessageSendingType.RESERVATION) {
      throw new IllegalStateException("예약 메시지에만 예약 시간을 설정할 수 있습니다.");
    }
    this.scheduledAt = time;
  }

  public void attachLink(boolean hasLink) {
    this.hasLink = hasLink;
  }

  public void updateReservation(
      String messageContent, MessageKind messageKind, Long customerId, LocalDateTime scheduledAt) {
    if (this.messageSendingType != MessageSendingType.RESERVATION) {
      throw new BusinessException(ErrorCode.INVALID_MESSAGET_TYPE);
    }
    if (this.messageDeliveryStatus != MessageDeliveryStatus.PENDING) {
      throw new BusinessException(ErrorCode.ALREADY_SENT_OR_CANCELED);
    }
    this.messageContent = messageContent;
    this.messageKind = messageKind;
    this.customerId = customerId;
    this.scheduledAt = scheduledAt;
  }

  public boolean isReservable() {
    return this.messageSendingType == MessageSendingType.RESERVATION
        && this.messageDeliveryStatus == MessageDeliveryStatus.PENDING
        && this.scheduledAt != null
        && this.scheduledAt.isAfter(LocalDateTime.now());
  }
}
