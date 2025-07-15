package com.deveagles.be15_deveagles_be.features.sales.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.PrepaidPassSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SessionPassSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.service.SalesQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원권 매출 조회", description = "회원권 매출 상세 조회 API")
@RestController
@RequestMapping("/membership-sales")
@RequiredArgsConstructor
public class MembershipSalesQueryController {

  private final SalesQueryService salesQueryService;

  @GetMapping("/prepaid/{prepaidPassSalesId}")
  @Operation(summary = "선불권 매출 상세 조회", description = "상품 매출 ID로 선불권 매출 상세 내역을 조회합니다.")
  public ResponseEntity<ApiResponse<PrepaidPassSalesDetailResponse>> getPrepaidPassSalesDetail(
      @PathVariable Long prepaidPassSalesId) {
    PrepaidPassSalesDetailResponse response =
        salesQueryService.getPrepaidPassSalesDetail(prepaidPassSalesId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/session/{sessionPassSalesId}")
  @Operation(summary = "횟수권 매출 상세 조회", description = "상품 매출 ID로 횟수권 매출 상세 내역을 조회합니다.")
  public ResponseEntity<ApiResponse<SessionPassSalesDetailResponse>> getSessionPassSalesDetail(
      @PathVariable Long sessionPassSalesId) {
    SessionPassSalesDetailResponse response =
        salesQueryService.getSessionPassSalesDetail(sessionPassSalesId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
