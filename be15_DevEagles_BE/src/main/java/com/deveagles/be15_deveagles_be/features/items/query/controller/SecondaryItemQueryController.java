package com.deveagles.be15_deveagles_be.features.items.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.items.query.dto.response.SecondaryItemResponse;
import com.deveagles.be15_deveagles_be.features.items.query.service.SecondaryItemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2차 상품 조회", description = "2차 상품 조회 API")
@RestController
@RequestMapping("/secondary-items")
@RequiredArgsConstructor
public class SecondaryItemQueryController {

  private final SecondaryItemQueryService secondaryItemQueryService;

  @GetMapping
  @Operation(summary = "2차 상품 전체 조회", description = "내 매장의 2차 상품 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<SecondaryItemResponse>>> getAllSecondaryItems(
      @AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    List<SecondaryItemResponse> response = secondaryItemQueryService.getAllSecondaryItems(shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/active")
  @Operation(summary = "2차 상품 활성화 목록 조회", description = "내 매장의 활성화된 2차 상품 목록만 조회합니다.")
  public ResponseEntity<ApiResponse<List<SecondaryItemResponse>>> getActiveSecondaryItems(
      @AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    List<SecondaryItemResponse> response =
        secondaryItemQueryService.getActiveSecondaryItems(shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/active/{shopId}")
  @Operation(
      summary = "2차 상품 활성화 목록 조회 (고객 예약 전용)",
      description = "shopId를 파라미터로 전달하여 해당 매장의 활성화된 2차 상품 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<SecondaryItemResponse>>> getActiveSecondaryItemsPublic(
      @PathVariable("shopId") Long shopId) {
    List<SecondaryItemResponse> response =
        secondaryItemQueryService.getActiveSecondaryItems(shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
