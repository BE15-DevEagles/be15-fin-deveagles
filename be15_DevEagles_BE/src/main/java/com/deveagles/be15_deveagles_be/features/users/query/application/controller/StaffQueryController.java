package com.deveagles.be15_deveagles_be.features.users.query.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.users.query.application.dto.request.GetStaffsListRequest;
import com.deveagles.be15_deveagles_be.features.users.query.application.dto.response.StaffsListResponse;
import com.deveagles.be15_deveagles_be.features.users.query.application.service.StaffQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/staffs")
@Tag(name = "직원 관리", description = "직원 관리 관련 API")
public class StaffQueryController {

  private final StaffQueryService staffQueryService;

  @GetMapping()
  @Operation(
      summary = "직원 목록 조회 (로그인 기반)",
      description = "현재 로그인한 사용자의 shopId를 기준으로 직원 목록을 조회합니다. ")
  public ResponseEntity<ApiResponse<StaffsListResponse>> getStaff(
      @AuthenticationPrincipal CustomUser customUser,
      @ModelAttribute GetStaffsListRequest request,
      @RequestParam(value = "shopId", required = false) Long shopId) {

    Long targetShopId = customUser != null ? customUser.getShopId() : shopId;

    if (targetShopId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("UNAUTHORIZED", "로그인이 필요합니다."));
    }

    StaffsListResponse response =
        staffQueryService.getStaff(
            targetShopId, request.size(), request.page(), request.keyword(), request.isActive());

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @GetMapping("/public/{shopId}")
  @Operation(
      summary = "직원 목록 조회 (고객 예약 전용)",
      description = "shopId를 파라미터로 전달하여 해당 매장의 직원 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<StaffsListResponse>> getStaffByShopId(
      @PathVariable("shopId") Long shopId, @ModelAttribute GetStaffsListRequest request) {

    StaffsListResponse response =
        staffQueryService.getStaff(
            shopId, request.size(), request.page(), request.keyword(), request.isActive());

    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
