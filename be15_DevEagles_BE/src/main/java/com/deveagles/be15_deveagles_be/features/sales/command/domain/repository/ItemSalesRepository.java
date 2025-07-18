package com.deveagles.be15_deveagles_be.features.sales.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.ItemSales;
import java.util.List;
import java.util.Optional;

public interface ItemSalesRepository {
  ItemSales save(ItemSales build);

  Optional<ItemSales> findBySalesId(Long salesId);

  List<ItemSales> findAllBySalesId(Long salesId);
}
