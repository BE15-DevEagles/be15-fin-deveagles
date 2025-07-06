package com.deveagles.be15_deveagles_be.features.sales.query.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesListResponse {
  private Long salesId;
  private LocalDateTime salesDate;
  private String salesType; // 상품 / 회원권 / 환불
  private String staffName;
  private String customerName;
  private String secondaryItemName;
  private Integer retailPrice;
  private Integer discountAmount;
  private Integer totalAmount;

  private List<PaymentsDTO> payments;
}
