package com.deveagles.be15_deveagles_be.features.sales.query.service;

import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResult;

public interface SalesQueryService {
  SalesListResult getSalesList(Long shopId, SalesListFilterRequest filter);
}
