package com.deveagles.be15_deveagles_be.features.sales.query.dto.response;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesListResult {
  private List<SalesListResponse> list;
  private Pagination pagination;
}
