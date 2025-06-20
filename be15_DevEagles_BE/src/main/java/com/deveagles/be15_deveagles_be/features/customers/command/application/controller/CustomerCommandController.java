package com.deveagles.be15_deveagles_be.features.customers.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "고객 관리", description = "고객 생성, 수정, 삭제 API")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CustomerCommandController {

  private final CustomerCommandService customerCommandService;

  @Operation(summary = "고객 생성", description = "새로운 고객을 등록합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "고객 생성 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 데이터"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복된 전화번호")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> createCustomer(
      @Parameter(description = "고객 생성 정보", required = true) @Valid @RequestBody
          CreateCustomerRequest request) {
    log.info(
        "고객 생성 요청 - 이름: {}, 전화번호: {}, 매장ID: {}",
        request.customerName(),
        request.phoneNumber(),
        request.shopId());

    CustomerCommandResponse response = customerCommandService.createCustomer(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
  }

  @Operation(summary = "고객 정보 수정", description = "기존 고객의 정보를 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 정보 수정 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 데이터")
  })
  @PutMapping("/{customerId}")
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> updateCustomer(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "고객 수정 정보", required = true) @Valid @RequestBody
          UpdateCustomerRequest request) {
    log.info("고객 정보 수정 요청 - 고객ID: {}, 이름: {}", customerId, request.customerName());

    // customerId를 request에 설정
    UpdateCustomerRequest updatedRequest =
        new UpdateCustomerRequest(
            customerId,
            request.customerName(),
            request.phoneNumber(),
            request.memo(),
            request.gender(),
            request.channelId());

    CustomerCommandResponse response = customerCommandService.updateCustomer(updatedRequest);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "고객 삭제", description = "고객을 소프트 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 삭제 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "이미 삭제된 고객")
  })
  @DeleteMapping("/{customerId}")
  public ResponseEntity<ApiResponse<String>> deleteCustomer(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "매장 ID", required = true) @RequestParam Long shopId) {
    log.info("고객 삭제 요청 - 고객ID: {}, 매장ID: {}", customerId, shopId);

    customerCommandService.deleteCustomer(customerId, shopId);
    return ResponseEntity.ok(ApiResponse.success("고객이 성공적으로 삭제되었습니다."));
  }

  @Operation(summary = "마케팅 동의 변경", description = "고객의 마케팅 수신 동의 상태를 변경합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "마케팅 동의 상태 변경 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @PatchMapping("/{customerId}/marketing-consent")
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> updateMarketingConsent(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "매장 ID", required = true) @RequestParam Long shopId,
      @Parameter(description = "마케팅 동의 여부", required = true) @RequestParam Boolean consent) {
    log.info("마케팅 동의 변경 요청 - 고객ID: {}, 매장ID: {}, 동의: {}", customerId, shopId, consent);

    CustomerCommandResponse response =
        customerCommandService.updateMarketingConsent(customerId, shopId, consent);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "알림 동의 변경", description = "고객의 알림 수신 동의 상태를 변경합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "알림 동의 상태 변경 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @PatchMapping("/{customerId}/notification-consent")
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> updateNotificationConsent(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "매장 ID", required = true) @RequestParam Long shopId,
      @Parameter(description = "알림 동의 여부", required = true) @RequestParam Boolean consent) {
    log.info("알림 동의 변경 요청 - 고객ID: {}, 매장ID: {}, 동의: {}", customerId, shopId, consent);

    CustomerCommandResponse response =
        customerCommandService.updateNotificationConsent(customerId, shopId, consent);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "방문 추가", description = "고객의 방문 정보를 추가합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "방문 추가 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @PatchMapping("/{customerId}/visit")
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> addVisit(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "매장 ID", required = true) @RequestParam Long shopId,
      @Parameter(description = "매출액", required = true) @RequestParam Integer revenue) {
    log.info("방문 추가 요청 - 고객ID: {}, 매장ID: {}, 매출: {}", customerId, shopId, revenue);

    CustomerCommandResponse response = customerCommandService.addVisit(customerId, shopId, revenue);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "노쇼 추가", description = "고객의 노쇼 정보를 추가합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "노쇼 추가 성공",
        content = @Content(schema = @Schema(implementation = CustomerCommandResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @PatchMapping("/{customerId}/noshow")
  public ResponseEntity<ApiResponse<CustomerCommandResponse>> addNoshow(
      @Parameter(description = "고객 ID", required = true) @PathVariable Long customerId,
      @Parameter(description = "매장 ID", required = true) @RequestParam Long shopId) {
    log.info("노쇼 추가 요청 - 고객ID: {}, 매장ID: {}", customerId, shopId);

    CustomerCommandResponse response = customerCommandService.addNoshow(customerId, shopId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
