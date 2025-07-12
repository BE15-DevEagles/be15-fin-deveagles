package com.deveagles.be15_deveagles_be.features.statistics.query.service;

import com.deveagles.be15_deveagles_be.features.statistics.query.dto.AdvancedSalesStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.DailyVisitorStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.HourlyVisitorStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationRequest;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationSummaryResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.SalesStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.SalesSummaryResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.StatisticsRequest;
import java.time.LocalDate;
import java.util.List;

public interface StatisticsQueryService {
  List<SalesStatisticsResponse> getSalesStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<AdvancedSalesStatisticsResponse> getAdvancedSalesStatistics(
      Long shopId, StatisticsRequest request);

  SalesSummaryResponse getSalesSummary(Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> getReservationStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> getAdvancedReservationStatistics(
      Long shopId, ReservationRequest request);

  ReservationSummaryResponse getReservationSummary(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> getStaffReservationStatistics(
      Long shopId, Long staffId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> getAllStaffReservationStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<HourlyVisitorStatisticsResponse> getHourlyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<DailyVisitorStatisticsResponse> getDailyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<AdvancedSalesStatisticsResponse> getPrimaryItemDailyTrend(
      Long shopId, LocalDate startDate, LocalDate endDate);
}
