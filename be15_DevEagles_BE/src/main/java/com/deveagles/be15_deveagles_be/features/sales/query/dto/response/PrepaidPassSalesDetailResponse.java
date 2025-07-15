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
public class PrepaidPassSalesDetailResponse {

  // sales
  private Long salesId;
  private String salesDate;
  private Long customerId;
  private String customerName;
  private String staffName;
  private Integer retailPrice;
  private Integer salesDiscountRate;
  private Integer discountAmount;
  private Integer totalAmount;
  private String salesMemo;

  // prepaid_pass_sales
  private Long prepaidPassSalesId;
  private Long prepaidPassId;

  // prepaid_pass
  private String prepaidPassName;
  private Integer prepaidPassPrice;
  private Integer bonus;
  private Integer discountRate;

  // payments
  private List<PaymentsDTO> payments;

  // customer_prepaid_pass
  private Long customerPrepaidPassId;
  private LocalDateTime createdAt;
}
