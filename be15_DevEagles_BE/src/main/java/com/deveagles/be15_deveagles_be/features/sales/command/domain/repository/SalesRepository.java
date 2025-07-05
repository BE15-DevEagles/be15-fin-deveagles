package com.deveagles.be15_deveagles_be.features.sales.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.Sales;
import java.util.Optional;

public interface SalesRepository {
  Sales save(Sales sales);

  Optional<Sales> findById(Long salesId);
}
