package com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemSalesRequest {

  @NotNull(message = "상품ID를 입력해주세요") private Long secondaryItemId;

  @NotNull(message = "고객ID를 입력해주세요") private Long customerId;

  @NotNull(message = "직원ID를 입력해주세요") private Long staffId;

  @NotNull(message = "매장ID를 입력해주세요") private Long shopId;

  private Long reservationId;

  private Integer discountRate;

  private Long couponId;

  @NotNull(message = "수량을 입력해주세요") private Integer quantity;

  @NotNull(message = "정가를 입력해주세요") private Integer retailPrice;

  private Integer discountAmount;

  @NotNull(message = "결제금액을 입력해주세요") private Integer totalAmount;

  private String salesMemo;

  @NotNull(message = "판매일시를 입력해주세요") private LocalDateTime salesDate;

  @NotNull(message = "결제수단을 입력해주세요") private List<PaymentsInfo> payments;
}
