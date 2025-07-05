package com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Sales {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sales_id")
  private Long salesId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "staff_id", nullable = false)
  private Long staffId;

  @Column(name = "shop_id", nullable = false)
  private Long shopId;

  @Column(name = "reservation_id")
  private Long reservationId;

  @Column(name = "discount_rate")
  private Integer discountRate;

  @Column(name = "retail_price")
  private Integer retailPrice;

  @Column(name = "discount_amount")
  private Integer discountAmount;

  @Column(name = "total_amount", nullable = false)
  private Integer totalAmount;

  @Column(name = "sales_memo", length = 255)
  private String salesMemo;

  @Column(name = "sales_date", nullable = false)
  private LocalDateTime salesDate;

  @Column(name = "is_refunded", nullable = false)
  private Boolean isRefunded = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "modified_at", nullable = false)
  private LocalDateTime modifiedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void update(
      Long shopId,
      Long customerId,
      Long staffId,
      Long reservationId,
      Integer retailPrice,
      Integer discountRate,
      Integer discountAmount,
      Integer totalAmount,
      String salesMemo,
      LocalDateTime salesDate) {
    this.shopId = shopId;
    this.customerId = customerId;
    this.staffId = staffId;
    this.reservationId = reservationId;
    this.retailPrice = retailPrice;
    this.discountRate = discountRate;
    this.discountAmount = discountAmount;
    this.totalAmount = totalAmount;
    this.salesMemo = salesMemo;
    this.salesDate = salesDate;
  }
}
