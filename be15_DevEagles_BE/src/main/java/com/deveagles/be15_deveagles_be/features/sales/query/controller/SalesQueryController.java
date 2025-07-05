package com.deveagles.be15_deveagles_be.features.sales.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResult;
import com.deveagles.be15_deveagles_be.features.sales.query.service.SalesQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매출 조회", description = "매출 내역 조회 API")
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesQueryController {

  private final SalesQueryService salesQueryService;

  @GetMapping
  @Operation(summary = "매출 목록 조회", description = "필터 조건에 따라 매장의 매출 내역을 조회합니다.")
  public ResponseEntity<ApiResponse<SalesListResult>> getSalesList(
      @AuthenticationPrincipal CustomUser customUser,
      @ModelAttribute SalesListFilterRequest filter) {

    Long shopId = customUser.getShopId();
    SalesListResult result = salesQueryService.getSalesList(shopId, filter);
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
