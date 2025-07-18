package com.deveagles.be15_deveagles_be.features.sales.query.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesListDTO {

  private Long SalesId;
  private Long itemSalesId;
  private String secondaryItemPrice;
  private String secondaryItemName;
}
