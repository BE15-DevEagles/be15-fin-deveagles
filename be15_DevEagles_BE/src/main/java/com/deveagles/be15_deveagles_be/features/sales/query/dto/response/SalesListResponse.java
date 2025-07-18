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

  private Long prepaidPassSalesId;
  private Long sessionPassSalesId;

  private Long salesId;
  private LocalDateTime salesDate;
  private String salesType; // 상품 / 회원권 / 환불
  private Long staffId;
  private String staffName;
  private String customerName;
  private String prepaidPassName;
  private String sessionPassName;
  private Integer retailPrice;
  private Integer discountAmount;
  private Integer totalAmount;

  private List<ItemSalesListDTO> items;
  private List<PaymentsDTO> payments;
}
