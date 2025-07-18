package com.deveagles.be15_deveagles_be.features.sales.query.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesDetailDTO {
  // item_sales
  private Long salesId;
  private Long itemSalesId;
  private Long secondaryItemId;
  private String secondaryItemName;
  private String secondaryItemPrice;
  private Integer itemDiscountRate;
  private Integer quantity;
}
