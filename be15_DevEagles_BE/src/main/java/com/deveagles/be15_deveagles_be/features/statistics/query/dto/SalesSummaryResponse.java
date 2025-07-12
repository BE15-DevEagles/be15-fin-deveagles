package com.deveagles.be15_deveagles_be.features.statistics.query.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryResponse {
  private Long totalSales;
  private Double dailyAverage;
  private Long totalTransactions;
  private Double averageOrderValue;
  private Double monthlyGrowth;
  private LocalDate startDate;
  private LocalDate endDate;
}
