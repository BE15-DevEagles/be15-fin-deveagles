package com.deveagles.be15_deveagles_be.features.statistics.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsQueryServiceImplTest {

  @Mock private StatisticsQueryRepository statisticsQueryRepository;

  @InjectMocks private StatisticsQueryServiceImpl statisticsQueryService;

  private final Long shopId = 1L;
  private final LocalDate startDate = LocalDate.of(2024, 1, 1);
  private final LocalDate endDate = LocalDate.of(2024, 1, 31);

  @Test
  @DisplayName("매출 통계 조회 테스트")
  void getSalesStatistics() {
    List<SalesStatisticsResponse> expectedResponse =
        List.of(
            SalesStatisticsResponse.builder()
                .date(startDate)
                .totalSalesAmount(100000L)
                .totalTransactions(10L)
                .build());

    when(statisticsQueryRepository.findSalesStatisticsByPeriod(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<SalesStatisticsResponse> result =
        statisticsQueryService.getSalesStatistics(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findSalesStatisticsByPeriod(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("고급 매출 통계 조회 테스트")
  void getAdvancedSalesStatistics() {
    StatisticsRequest request = new StatisticsRequest();
    request.setStartDate(startDate);
    request.setEndDate(endDate);
    request.setGroupBy(StatisticsRequest.GroupBy.DAY);

    List<AdvancedSalesStatisticsResponse> expectedResponse =
        List.of(
            AdvancedSalesStatisticsResponse.builder()
                .date("2024-01-01")
                .totalSalesAmount(100000L)
                .totalTransactions(10L)
                .totalDiscountAmount(5000L)
                .totalCouponDiscountAmount(2000L)
                .build());

    when(statisticsQueryRepository.findAdvancedSalesStatistics(shopId, request))
        .thenReturn(expectedResponse);

    List<AdvancedSalesStatisticsResponse> result =
        statisticsQueryService.getAdvancedSalesStatistics(shopId, request);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findAdvancedSalesStatistics(shopId, request);
  }

  @Test
  @DisplayName("매출 요약 통계 조회 테스트")
  void getSalesSummary() {
    SalesSummaryResponse expectedResponse =
        SalesSummaryResponse.builder()
            .totalSales(1000000L)
            .dailyAverage(32258.06)
            .totalTransactions(100L)
            .averageOrderValue(10000.0)
            .monthlyGrowth(15.5)
            .startDate(startDate)
            .endDate(endDate)
            .build();

    when(statisticsQueryRepository.findSalesSummary(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    SalesSummaryResponse result =
        statisticsQueryService.getSalesSummary(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findSalesSummary(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("예약 통계 조회 테스트")
  void getReservationStatistics() {
    List<ReservationStatisticsResponse> expectedResponse =
        List.of(
            new ReservationStatisticsResponse(
                startDate,
                LocalTime.of(10, 0),
                20,
                15,
                5,
                BigDecimal.valueOf(75.00),
                "DAY",
                "2024-01-01",
                null,
                null));

    when(statisticsQueryRepository.findReservationStatisticsByPeriod(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<ReservationStatisticsResponse> result =
        statisticsQueryService.getReservationStatistics(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findReservationStatisticsByPeriod(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("고급 예약 통계 조회 테스트")
  void getAdvancedReservationStatistics() {
    ReservationRequest request = new ReservationRequest();
    request.setStartDate(startDate);
    request.setEndDate(endDate);
    request.setGroupBy(ReservationRequest.GroupBy.DAY);

    List<ReservationStatisticsResponse> expectedResponse =
        List.of(
            new ReservationStatisticsResponse(
                startDate,
                LocalTime.of(10, 0),
                20,
                15,
                5,
                BigDecimal.valueOf(75.00),
                "DAY",
                "2024-01-01",
                null,
                null));

    when(statisticsQueryRepository.findAdvancedReservationStatistics(shopId, request))
        .thenReturn(expectedResponse);

    List<ReservationStatisticsResponse> result =
        statisticsQueryService.getAdvancedReservationStatistics(shopId, request);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findAdvancedReservationStatistics(shopId, request);
  }

  @Test
  @DisplayName("예약 요약 통계 조회 테스트")
  void getReservationSummary() {
    ReservationSummaryResponse expectedResponse =
        new ReservationSummaryResponse(
            620,
            465,
            155,
            BigDecimal.valueOf(75.00),
            31,
            BigDecimal.valueOf(15.00),
            200,
            265,
            BigDecimal.valueOf(80.00),
            BigDecimal.valueOf(70.00));

    when(statisticsQueryRepository.findReservationSummary(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    ReservationSummaryResponse result =
        statisticsQueryService.getReservationSummary(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findReservationSummary(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("특정 직원 예약 통계 조회 테스트")
  void getStaffReservationStatistics() {
    Long staffId = 1L;
    List<ReservationStatisticsResponse> expectedResponse =
        List.of(
            new ReservationStatisticsResponse(
                startDate,
                LocalTime.of(10, 0),
                20,
                10,
                10,
                BigDecimal.valueOf(50.00),
                "STAFF",
                "2024-01-01",
                staffId,
                "직원1"));

    when(statisticsQueryRepository.findStaffReservationStatistics(
            shopId, staffId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<ReservationStatisticsResponse> result =
        statisticsQueryService.getStaffReservationStatistics(shopId, staffId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository)
        .findStaffReservationStatistics(shopId, staffId, startDate, endDate);
  }

  @Test
  @DisplayName("모든 직원 예약 통계 조회 테스트")
  void getAllStaffReservationStatistics() {
    List<ReservationStatisticsResponse> expectedResponse =
        List.of(
            new ReservationStatisticsResponse(
                startDate,
                LocalTime.of(10, 0),
                20,
                10,
                10,
                BigDecimal.valueOf(50.00),
                "STAFF",
                "직원1",
                1L,
                "직원1"),
            new ReservationStatisticsResponse(
                startDate,
                LocalTime.of(10, 0),
                20,
                8,
                12,
                BigDecimal.valueOf(40.00),
                "STAFF",
                "직원2",
                2L,
                "직원2"));

    when(statisticsQueryRepository.findAllStaffReservationStatistics(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<ReservationStatisticsResponse> result =
        statisticsQueryService.getAllStaffReservationStatistics(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findAllStaffReservationStatistics(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("시간대별 방문객 통계 조회 테스트")
  void getHourlyVisitorStatistics() {
    List<HourlyVisitorStatisticsResponse> expectedResponse =
        List.of(
            new HourlyVisitorStatisticsResponse(10, "10:00", 5L, 8L, 13L, "10시"),
            new HourlyVisitorStatisticsResponse(14, "14:00", 3L, 7L, 10L, "14시"));

    when(statisticsQueryRepository.findHourlyVisitorStatistics(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<HourlyVisitorStatisticsResponse> result =
        statisticsQueryService.getHourlyVisitorStatistics(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findHourlyVisitorStatistics(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("일별 방문객 통계 조회 테스트")
  void getDailyVisitorStatistics() {
    List<DailyVisitorStatisticsResponse> expectedResponse =
        List.of(
            new DailyVisitorStatisticsResponse(startDate, "월", 10L, 15L, 25L, "01/01"),
            new DailyVisitorStatisticsResponse(startDate.plusDays(1), "화", 8L, 12L, 20L, "01/02"));

    when(statisticsQueryRepository.findDailyVisitorStatistics(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<DailyVisitorStatisticsResponse> result =
        statisticsQueryService.getDailyVisitorStatistics(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findDailyVisitorStatistics(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("1차 상품별 일별 매출추이 조회 테스트")
  void getPrimaryItemDailyTrend() {
    List<AdvancedSalesStatisticsResponse> expectedResponse =
        List.of(
            AdvancedSalesStatisticsResponse.builder()
                .date("2024-01-01")
                .primaryItemName("헤어컷")
                .totalSalesAmount(50000L)
                .totalTransactions(5L)
                .totalDiscountAmount(2500L)
                .totalCouponDiscountAmount(1000L)
                .build(),
            AdvancedSalesStatisticsResponse.builder()
                .date("2024-01-01")
                .primaryItemName("펌")
                .totalSalesAmount(80000L)
                .totalTransactions(2L)
                .totalDiscountAmount(4000L)
                .totalCouponDiscountAmount(2000L)
                .build());

    when(statisticsQueryRepository.findPrimaryItemDailyTrend(shopId, startDate, endDate))
        .thenReturn(expectedResponse);

    List<AdvancedSalesStatisticsResponse> result =
        statisticsQueryService.getPrimaryItemDailyTrend(shopId, startDate, endDate);

    assertThat(result).isEqualTo(expectedResponse);
    verify(statisticsQueryRepository).findPrimaryItemDailyTrend(shopId, startDate, endDate);
  }

  @Test
  @DisplayName("Repository 파라미터 전달 검증 테스트")
  void verifyRepositoryParameterPassing() {
    StatisticsRequest statisticsRequest = new StatisticsRequest();
    statisticsRequest.setGroupBy(StatisticsRequest.GroupBy.CATEGORY);

    ReservationRequest reservationRequest = new ReservationRequest();
    reservationRequest.setGroupBy(ReservationRequest.GroupBy.HOUR);

    Long staffId = 10L;

    when(statisticsQueryRepository.findSalesStatisticsByPeriod(any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findAdvancedSalesStatistics(any(), any())).thenReturn(List.of());
    when(statisticsQueryRepository.findSalesSummary(any(), any(), any()))
        .thenReturn(SalesSummaryResponse.builder().build());
    when(statisticsQueryRepository.findReservationStatisticsByPeriod(any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findAdvancedReservationStatistics(any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findReservationSummary(any(), any(), any()))
        .thenReturn(new ReservationSummaryResponse());
    when(statisticsQueryRepository.findStaffReservationStatistics(any(), any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findAllStaffReservationStatistics(any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findHourlyVisitorStatistics(any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findDailyVisitorStatistics(any(), any(), any()))
        .thenReturn(List.of());
    when(statisticsQueryRepository.findPrimaryItemDailyTrend(any(), any(), any()))
        .thenReturn(List.of());

    statisticsQueryService.getSalesStatistics(shopId, startDate, endDate);
    statisticsQueryService.getAdvancedSalesStatistics(shopId, statisticsRequest);
    statisticsQueryService.getSalesSummary(shopId, startDate, endDate);
    statisticsQueryService.getReservationStatistics(shopId, startDate, endDate);
    statisticsQueryService.getAdvancedReservationStatistics(shopId, reservationRequest);
    statisticsQueryService.getReservationSummary(shopId, startDate, endDate);
    statisticsQueryService.getStaffReservationStatistics(shopId, staffId, startDate, endDate);
    statisticsQueryService.getAllStaffReservationStatistics(shopId, startDate, endDate);
    statisticsQueryService.getHourlyVisitorStatistics(shopId, startDate, endDate);
    statisticsQueryService.getDailyVisitorStatistics(shopId, startDate, endDate);
    statisticsQueryService.getPrimaryItemDailyTrend(shopId, startDate, endDate);

    verify(statisticsQueryRepository)
        .findSalesStatisticsByPeriod(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findAdvancedSalesStatistics(eq(shopId), eq(statisticsRequest));
    verify(statisticsQueryRepository).findSalesSummary(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findReservationStatisticsByPeriod(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findAdvancedReservationStatistics(eq(shopId), eq(reservationRequest));
    verify(statisticsQueryRepository)
        .findReservationSummary(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findStaffReservationStatistics(eq(shopId), eq(staffId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findAllStaffReservationStatistics(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findHourlyVisitorStatistics(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findDailyVisitorStatistics(eq(shopId), eq(startDate), eq(endDate));
    verify(statisticsQueryRepository)
        .findPrimaryItemDailyTrend(eq(shopId), eq(startDate), eq(endDate));
  }
}
