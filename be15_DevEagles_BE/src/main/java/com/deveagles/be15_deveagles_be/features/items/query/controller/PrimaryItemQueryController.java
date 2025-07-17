package com.deveagles.be15_deveagles_be.features.items.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.items.query.dto.response.PrimaryItemResponse;
import com.deveagles.be15_deveagles_be.features.items.query.service.PrimaryItemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1차 상품 조회", description = "1차 상품 조회 API")
@RestController
@RequestMapping("/primary-items")
@RequiredArgsConstructor
public class PrimaryItemQueryController {

  private final PrimaryItemQueryService primaryItemQueryService;

  @GetMapping
  @Operation(summary = "1차 상품 전체 조회", description = "내 매장의 1차 상품 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<PrimaryItemResponse>>> getAllPrimaryItems(
      @AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    List<PrimaryItemResponse> response = primaryItemQueryService.getAllPrimaryItems(shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/{shopId}")
  @Operation(
      summary = "1차 상품 전체 조회 (고객 예약 전용)",
      description = "shopId를 파라미터로 받아 해당 매장의 1차 상품 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<PrimaryItemResponse>>> getAllPrimaryItemsPublic(
      @PathVariable("shopId") Long shopId) {
    List<PrimaryItemResponse> response = primaryItemQueryService.getAllPrimaryItems(shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
