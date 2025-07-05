package com.deveagles.be15_deveagles_be.features.sales.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.SessionPassSales;

public interface SessionPassSalesRepository {
  SessionPassSales save(SessionPassSales sessionPassSales);

  void deleteBySalesId(Long salesId);
}
