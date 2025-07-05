package com.deveagles.be15_deveagles_be.features.sales.command.application.service;

import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.SessionPassSalesRequest;
import jakarta.transaction.Transactional;

public interface SessionPassSalesCommandService {
  void registSessionPassSales(SessionPassSalesRequest request);

  @Transactional
  void updateSessionPassSales(Long salesId, SessionPassSalesRequest request);
}
