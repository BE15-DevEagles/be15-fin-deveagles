package com.deveagles.be15_deveagles_be.features.sales.command.infrastructure.repository;

import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.ItemSales;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.ItemSalesRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaItemSalesRepository
    extends JpaRepository<ItemSales, Long>, ItemSalesRepository {}
