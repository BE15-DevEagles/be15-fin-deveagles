package com.deveagles.be15_deveagles_be.features.sales.command.application.service;

import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.ItemSalesRequest;
import jakarta.transaction.Transactional;

public interface ItemSalesCommandService {

  void registItemSales(ItemSalesRequest request);

  @Transactional
  void updateItemSales(Long salesId, ItemSalesRequest request);

  @Transactional
  void refundItemSales(Long salesId);
}
