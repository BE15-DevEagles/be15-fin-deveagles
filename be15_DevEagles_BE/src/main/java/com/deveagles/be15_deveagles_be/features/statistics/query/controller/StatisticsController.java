package com.deveagles.be15_deveagles_be.features.statistics.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.AdvancedSalesStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.DailyVisitorStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.HourlyVisitorStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationRequest;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.ReservationSummaryResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.SalesStatisticsResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.SalesSummaryResponse;
import com.deveagles.be15_deveagles_be.features.statistics.query.dto.StatisticsRequest;
import com.deveagles.be15_deveagles_be.features.statistics.query.service.StatisticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics", description = "통계 관련 API")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

  private final StatisticsQueryService statisticsQueryService;

  @GetMapping("/sales")
  @Operation(summary = "매출 통계 조회", description = "기간별 매출 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<SalesStatisticsResponse>>> getSalesStatistics(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<SalesStatisticsResponse> statistics =
        statisticsQueryService.getSalesStatistics(customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @PostMapping("/sales/advanced")
  @Operation(summary = "고급 매출 통계 조회", description = "다양한 조건으로 그룹화하고 필터링된 매출 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<AdvancedSalesStatisticsResponse>>>
      getAdvancedSalesStatistics(
          @AuthenticationPrincipal CustomUser customUser, @RequestBody StatisticsRequest request) {
    List<AdvancedSalesStatisticsResponse> statistics =
        statisticsQueryService.getAdvancedSalesStatistics(customUser.getShopId(), request);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/sales/summary")
  @Operation(summary = "매출 요약 통계 조회", description = "총 매출, 일일 평균, 거래 건수 등의 요약 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<SalesSummaryResponse>> getSalesSummary(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    SalesSummaryResponse summary =
        statisticsQueryService.getSalesSummary(customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(summary));
  }

  @GetMapping("/sales/primary-item-daily-trend")
  @Operation(summary = "1차 상품별 일별 매출추이 조회", description = "1차 상품별 일별 매출 추이를 조회합니다.")
  public ResponseEntity<ApiResponse<List<AdvancedSalesStatisticsResponse>>>
      getPrimaryItemDailyTrend(
          @AuthenticationPrincipal CustomUser customUser,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<AdvancedSalesStatisticsResponse> statistics =
        statisticsQueryService.getPrimaryItemDailyTrend(customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/reservations")
  @Operation(summary = "예약률 통계 조회", description = "기간별 예약률 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<ReservationStatisticsResponse>>> getReservationStatistics(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<ReservationStatisticsResponse> statistics =
        statisticsQueryService.getReservationStatistics(customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @PostMapping("/reservations/advanced")
  @Operation(summary = "고급 예약률 통계 조회", description = "다양한 조건으로 그룹화하고 필터링된 예약률 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<ReservationStatisticsResponse>>>
      getAdvancedReservationStatistics(
          @AuthenticationPrincipal CustomUser customUser, @RequestBody ReservationRequest request) {
    List<ReservationStatisticsResponse> statistics =
        statisticsQueryService.getAdvancedReservationStatistics(customUser.getShopId(), request);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/reservations/summary")
  @Operation(summary = "예약률 요약 통계 조회", description = "총 예약률, 일일 평균, 피크 시간대 등의 요약 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<ReservationSummaryResponse>> getReservationSummary(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    ReservationSummaryResponse summary =
        statisticsQueryService.getReservationSummary(customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(summary));
  }

  @GetMapping("/reservations/staff/{staffId}")
  @Operation(summary = "특정 직원 예약률 통계 조회", description = "특정 직원의 예약률 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<ReservationStatisticsResponse>>>
      getStaffReservationStatistics(
          @AuthenticationPrincipal CustomUser customUser,
          @PathVariable Long staffId,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<ReservationStatisticsResponse> statistics =
        statisticsQueryService.getStaffReservationStatistics(
            customUser.getShopId(), staffId, startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/reservations/staff")
  @Operation(summary = "모든 직원 예약률 통계 조회", description = "모든 직원의 예약률 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<List<ReservationStatisticsResponse>>>
      getAllStaffReservationStatistics(
          @AuthenticationPrincipal CustomUser customUser,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<ReservationStatisticsResponse> statistics =
        statisticsQueryService.getAllStaffReservationStatistics(
            customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/visitors/hourly")
  @Operation(summary = "시간대별 방문객 성별 통계 조회", description = "시간대별 방문객 수를 성별로 구분하여 조회합니다.")
  public ResponseEntity<ApiResponse<List<HourlyVisitorStatisticsResponse>>>
      getHourlyVisitorStatistics(
          @AuthenticationPrincipal CustomUser customUser,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<HourlyVisitorStatisticsResponse> statistics =
        statisticsQueryService.getHourlyVisitorStatistics(
            customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }

  @GetMapping("/visitors/daily")
  @Operation(summary = "일별 방문객 성별 통계 조회", description = "일별 방문객 수를 성별로 구분하여 조회합니다.")
  public ResponseEntity<ApiResponse<List<DailyVisitorStatisticsResponse>>>
      getDailyVisitorStatistics(
          @AuthenticationPrincipal CustomUser customUser,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<DailyVisitorStatisticsResponse> statistics =
        statisticsQueryService.getDailyVisitorStatistics(
            customUser.getShopId(), startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success(statistics));
  }
}
