package com.deveagles.be15_deveagles_be.features.sales.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.ItemSalesDetailResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.service.SalesQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상품 매출 조회", description = "상품 매출 내역 조회 API")
@RestController
@RequestMapping("/item-sales")
@RequiredArgsConstructor
public class ItemSalesQureyController {

  private final SalesQueryService salesQueryService;

  @GetMapping("/{salesId}")
  @Operation(summary = "상품 매출 상세 조회", description = "매출 ID로 상품 매출 상세 내역을 조회합니다.")
  public ResponseEntity<ApiResponse<ItemSalesDetailResponse>> getItemSalesDetail(
      @PathVariable Long salesId) {
    ItemSalesDetailResponse response = salesQueryService.getItemSalesDetailBySalesId(salesId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
