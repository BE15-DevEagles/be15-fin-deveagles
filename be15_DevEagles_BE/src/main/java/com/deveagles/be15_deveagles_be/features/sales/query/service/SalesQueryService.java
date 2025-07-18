package com.deveagles.be15_deveagles_be.features.sales.query.service;

import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.ItemSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.PrepaidPassSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResult;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SessionPassSalesDetailResponse;

public interface SalesQueryService {
  SalesListResult getSalesList(Long shopId, SalesListFilterRequest filter);

  ItemSalesDetailResponse getItemSalesDetailBySalesId(Long salesId);

  PrepaidPassSalesDetailResponse getPrepaidPassSalesDetail(Long prepaidPassSalesId);

  SessionPassSalesDetailResponse getSessionPassSalesDetail(Long sessionPassSalesId);
}
