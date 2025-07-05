package com.deveagles.be15_deveagles_be.features.sales.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.SessionPassSalesRequest;
import com.deveagles.be15_deveagles_be.features.sales.command.application.service.SessionPassSalesCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "횟수권 매출", description = "횟수권 매출 API")
@RestController
@RequestMapping("session-pass-sales")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SessionPassSalesCommandController {

  private final SessionPassSalesCommandService sessionPassSalesCommandService;

  @Operation(summary = "횟수권 매출 등록", description = "횟수권을 매출을 등록합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> registerSessionPassSales(
      @Validated @RequestBody SessionPassSalesRequest request) {
    sessionPassSalesCommandService.registSessionPassSales(request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "횟수권 매출 수정", description = "횟수권 매출을 수정합니다.")
  @PutMapping("/{salesId}")
  public ResponseEntity<ApiResponse<Void>> updateSessionPassSales(
      @PathVariable Long salesId, @Validated @RequestBody SessionPassSalesRequest request) {

    sessionPassSalesCommandService.updateSessionPassSales(salesId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
