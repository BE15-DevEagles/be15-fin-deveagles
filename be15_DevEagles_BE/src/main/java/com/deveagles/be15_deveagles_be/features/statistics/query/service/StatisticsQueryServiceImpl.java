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
import com.deveagles.be15_deveagles_be.features.statistics.query.repository.StatisticsQueryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsQueryServiceImpl implements StatisticsQueryService {

  private final StatisticsQueryRepository statisticsQueryRepository;

  @Override
  public List<SalesStatisticsResponse> getSalesStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findSalesStatisticsByPeriod(shopId, startDate, endDate);
  }

  @Override
  public List<AdvancedSalesStatisticsResponse> getAdvancedSalesStatistics(
      Long shopId, StatisticsRequest request) {
    return statisticsQueryRepository.findAdvancedSalesStatistics(shopId, request);
  }

  @Override
  public SalesSummaryResponse getSalesSummary(Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findSalesSummary(shopId, startDate, endDate);
  }

  @Override
  public List<ReservationStatisticsResponse> getReservationStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findReservationStatisticsByPeriod(shopId, startDate, endDate);
  }

  @Override
  public List<ReservationStatisticsResponse> getAdvancedReservationStatistics(
      Long shopId, ReservationRequest request) {
    return statisticsQueryRepository.findAdvancedReservationStatistics(shopId, request);
  }

  @Override
  public ReservationSummaryResponse getReservationSummary(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findReservationSummary(shopId, startDate, endDate);
  }

  @Override
  public List<ReservationStatisticsResponse> getStaffReservationStatistics(
      Long shopId, Long staffId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findStaffReservationStatistics(
        shopId, staffId, startDate, endDate);
  }

  @Override
  public List<ReservationStatisticsResponse> getAllStaffReservationStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findAllStaffReservationStatistics(shopId, startDate, endDate);
  }

  @Override
  public List<HourlyVisitorStatisticsResponse> getHourlyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findHourlyVisitorStatistics(shopId, startDate, endDate);
  }

  @Override
  public List<DailyVisitorStatisticsResponse> getDailyVisitorStatistics(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findDailyVisitorStatistics(shopId, startDate, endDate);
  }

  @Override
  public List<AdvancedSalesStatisticsResponse> getPrimaryItemDailyTrend(
      Long shopId, LocalDate startDate, LocalDate endDate) {
    return statisticsQueryRepository.findPrimaryItemDailyTrend(shopId, startDate, endDate);
  }
}
