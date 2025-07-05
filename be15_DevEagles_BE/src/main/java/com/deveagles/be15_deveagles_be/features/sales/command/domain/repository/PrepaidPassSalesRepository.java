package com.deveagles.be15_deveagles_be.features.sales.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.PrepaidPassSales;

public interface PrepaidPassSalesRepository {
  PrepaidPassSales save(PrepaidPassSales prepaidPassSales);

  void deleteBySalesId(Long salesId);
}
