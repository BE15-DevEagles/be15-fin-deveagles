package com.deveagles.be15_deveagles_be.features.statistics.query.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesStatisticsResponse {
  private LocalDate date;
  private long totalSalesAmount;
  private long totalTransactions;
}
