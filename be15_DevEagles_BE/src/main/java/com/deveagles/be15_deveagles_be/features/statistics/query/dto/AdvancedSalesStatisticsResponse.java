package com.deveagles.be15_deveagles_be.features.statistics.query.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvancedSalesStatisticsResponse {
  // Grouping Keys
  private String date; // Could be day, week, or month
  private String gender;
  private String category;
  private String primaryItemName;
  private String secondaryItemName;

  // Aggregated Values
  private long totalSalesAmount;
  private long totalTransactions;
  private long totalDiscountAmount;
  private long totalCouponDiscountAmount;
}
