package com.deveagles.be15_deveagles_be.features.sales.query.dto.request;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesListFilterRequest {
  private LocalDate startDate;
  private LocalDate endDate;
  private List<String> saleTypes;
  private List<String> itemTypes;
  private Long staffId;
  private String customerKeyword;

  private Integer page = 1;
  private Integer size = 10;

  public int getOffset() {
    return (page - 1) * size;
  }
}
