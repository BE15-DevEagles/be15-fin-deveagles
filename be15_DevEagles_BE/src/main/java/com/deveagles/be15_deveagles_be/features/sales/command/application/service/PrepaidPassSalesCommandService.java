package com.deveagles.be15_deveagles_be.features.sales.command.application.service;

import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PrepaidPassSalesRequest;
import jakarta.transaction.Transactional;

public interface PrepaidPassSalesCommandService {
  void registPrepaidPassSales(PrepaidPassSalesRequest request);

  @Transactional
  void updatePrepaidPassSales(Long salesId, PrepaidPassSalesRequest request);
}
