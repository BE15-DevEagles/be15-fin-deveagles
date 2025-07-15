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
public class SessionPassSalesDetailResponse {

  // sales
  private Long salesId;
  private String salesDate;
  private String customerName;
  private String staffName;
  private Integer retailPrice;
  private Integer salesDiscountRate;
  private Integer discountAmount;
  private Integer totalAmount;
  private String salesMemo;

  // session_pass_sales
  private Long sessionPassSalesId;
  private Long sessionPassId;

  // session_pass
  private String sessionPassName;
  private Integer sessionPassPrice;
  private Integer session;
  private Integer bonus;
  private Integer discountRate;

  // payments
  private List<PaymentsDTO> payments;

  // customer_session_pass
  private Long customerSessionPassId;
  private LocalDateTime createdAt;
}
