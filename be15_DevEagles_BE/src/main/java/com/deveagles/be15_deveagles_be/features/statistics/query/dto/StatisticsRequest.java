package com.deveagles.be15_deveagles_be.features.statistics.query.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatisticsRequest {
  private LocalDate startDate;
  private LocalDate endDate;
  private GroupBy groupBy;
  private TimeRange timeRange;
  private String gender;
  private Long categoryId;
  private Long primaryItemId;
  private Long secondaryItemId;

  public enum GroupBy {
    DAY,
    WEEK,
    MONTH,
    GENDER,
    CATEGORY,
    PRIMARY_ITEM,
    SECONDARY_ITEM
  }

  public enum TimeRange {
    CUSTOM,
    LAST_WEEK,
    LAST_MONTH,
    LAST_6_MONTHS,
    LAST_YEAR
  }
}
