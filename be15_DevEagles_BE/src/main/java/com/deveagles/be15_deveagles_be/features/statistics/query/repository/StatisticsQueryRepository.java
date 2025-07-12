package com.deveagles.be15_deveagles_be.features.statistics.query.repository;

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

public interface StatisticsQueryRepository {
  List<SalesStatisticsResponse> findSalesStatisticsByPeriod(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<AdvancedSalesStatisticsResponse> findAdvancedSalesStatistics(
      Long shopId, StatisticsRequest request);

  SalesSummaryResponse findSalesSummary(Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> findReservationStatisticsByPeriod(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> findAdvancedReservationStatistics(
      Long shopId, ReservationRequest request);

  ReservationSummaryResponse findReservationSummary(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> findStaffReservationStatistics(
      Long shopId, Long staffId, LocalDate startDate, LocalDate endDate);

  List<ReservationStatisticsResponse> findAllStaffReservationStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<HourlyVisitorStatisticsResponse> findHourlyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<DailyVisitorStatisticsResponse> findDailyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate);

  List<AdvancedSalesStatisticsResponse> findPrimaryItemDailyTrend(
      Long shopId, LocalDate startDate, LocalDate endDate);
}
