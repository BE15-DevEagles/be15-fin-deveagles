package com.deveagles.be15_deveagles_be.features.membership.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerMemebershipFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.membership.query.service.CustomerMembershipQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원권 조회", description = "고객 회원권 관련 조회 API")
@RestController
@RequestMapping("/customer-memberships")
@RequiredArgsConstructor
public class CustomerMembershipQueryController {

  private final CustomerMembershipQueryService customerMembershipQueryService;

  @Operation(summary = "고객 회원권 전체 조회", description = "샵에 등록된 모든 고객의 회원권 정보를 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<CustomerMembershipResult>> getAllCustomerMemberships(
      @AuthenticationPrincipal CustomUser user,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {

    CustomerMembershipResult result =
        customerMembershipQueryService.getCustomerMembershipList(user.getShopId(), page, size);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "고객 회원권 필터 조회", description = "잔여 선불권 금액 등 조건에 따라 고객 회원권 정보를 조회합니다.")
  @PostMapping("/filter")
  public ResponseEntity<ApiResponse<CustomerMembershipResult>> getFilteredCustomerMemberships(
      @AuthenticationPrincipal CustomUser user,
      @RequestBody @Valid CustomerMemebershipFilterRequest filter) {

    CustomerMembershipResult result =
        customerMembershipQueryService.getCustomerMembershipList(user.getShopId(), filter);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "고객 보유 선불권 상세 조회", description = "특정 고객이 보유한 선불권 상세 목록을 조회합니다.")
  @GetMapping("/prepaid-passes/detail/{customerId}")
  public ResponseEntity<ApiResponse<List<CustomerPrepaidPassDetailInfo>>>
      getCustomerPrepaidPassDetails(@PathVariable Long customerId) {

    List<CustomerPrepaidPassDetailInfo> result =
        customerMembershipQueryService.getPrepaidPassDetailsByCustomerId(customerId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "고객 보유 횟수권 상세 조회", description = "특정 고객이 보유한 횟수권 상세 목록을 조회합니다.")
  @GetMapping("/session-passes/detail/{customerId}")
  public ResponseEntity<ApiResponse<List<CustomerSessionPassDetailInfo>>>
      getCustomerSessionPassDetails(@PathVariable Long customerId) {

    List<CustomerSessionPassDetailInfo> result =
        customerMembershipQueryService.getSessionPassDetailsByCustomerId(customerId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(
      summary = "만료/사용완료된 회원권 목록 조회",
      description = "해당 매장의 만료되었거나 잔여량이 0인 선불권 및 횟수권 목록을 조회합니다.")
  @GetMapping("/expired-or-used-up/{customerId}")
  public ResponseEntity<ApiResponse<CustomerExpiringMembershipResult>>
      getExpiredOrUsedUpMemberships(@PathVariable Long customerId) {

    CustomerExpiringMembershipResult result =
        customerMembershipQueryService.getExpiredOrUsedUpMemberships(customerId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(
      summary = "고객이 특정 상품에 대해 보유한 횟수권 목록 조회",
      description = "고객 ID와 2차 상품 ID를 기준으로 사용 가능한 횟수권 목록을 반환합니다.")
  @GetMapping("/session-pass/available")
  public ResponseEntity<ApiResponse<List<CustomerSessionPassReponse>>> getAvailableSessionPasses(
      @RequestParam Long customerId) {

    List<CustomerSessionPassReponse> result =
        customerMembershipQueryService.getAvailableSessionPassesByCustomerId(customerId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
