package com.deveagles.be15_deveagles_be.features.sales.query.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsDTO {

  private String paymentsMethod;
  private Integer amount;
}
