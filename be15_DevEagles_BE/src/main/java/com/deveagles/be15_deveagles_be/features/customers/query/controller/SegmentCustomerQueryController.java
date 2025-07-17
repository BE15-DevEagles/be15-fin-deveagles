package com.deveagles.be15_deveagles_be.features.customers.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.SegmentCustomersResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "세그먼트별 고객 조회", description = "세그먼트별 고객 ID 목록 조회 API")
@RestController
@RequestMapping("/segments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SegmentCustomerQueryController {

  private final CustomerQueryService customerQueryService;

  @Operation(summary = "세그먼트 태그별 고객 ID 조회", description = "특정 세그먼트 태그에 속한 고객 ID 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "세그먼트별 고객 조회 성공",
        content = @Content(schema = @Schema(implementation = SegmentCustomersResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세그먼트를 찾을 수 없음")
  })
  @GetMapping("/{segmentTag}/customers")
  public ResponseEntity<ApiResponse<SegmentCustomersResponse>> getCustomersBySegmentTag(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "세그먼트 태그 (예: NEW, GROWING, LOYAL, VIP, DORMANT)", required = true)
          @PathVariable
          String segmentTag) {

    log.info("세그먼트 태그별 고객 조회 요청: {}, 매장ID: {}", segmentTag, user.getShopId());

    SegmentCustomersResponse response =
        customerQueryService.getCustomersBySegmentTag(segmentTag.toUpperCase(), user.getShopId());

    log.info(
        "세그먼트 '{}' 조회 완료: {} 명의 고객 (매장 {})",
        segmentTag,
        response.customerCount(),
        user.getShopId());

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "세그먼트 ID별 고객 ID 조회", description = "특정 세그먼트 ID에 속한 고객 ID 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "세그먼트별 고객 조회 성공",
        content = @Content(schema = @Schema(implementation = SegmentCustomersResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세그먼트를 찾을 수 없음")
  })
  @GetMapping("/id/{segmentId}/customers")
  public ResponseEntity<ApiResponse<SegmentCustomersResponse>> getCustomersBySegmentId(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "세그먼트 ID", required = true) @PathVariable Long segmentId) {

    log.info("세그먼트 ID별 고객 조회 요청: {}, 매장ID: {}", segmentId, user.getShopId());

    SegmentCustomersResponse response =
        customerQueryService.getCustomersBySegmentId(segmentId, user.getShopId());

    log.info(
        "세그먼트 ID '{}' 조회 완료: {} 명의 고객 (매장 {})",
        segmentId,
        response.customerCount(),
        user.getShopId());

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "다중 세그먼트별 고객 ID 조회", description = "여러 세그먼트 태그에 속한 고객 ID 목록을 일괄 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "다중 세그먼트별 고객 조회 성공",
        content = @Content(schema = @Schema(implementation = SegmentCustomersResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 파라미터")
  })
  @GetMapping("/customers")
  public ResponseEntity<ApiResponse<List<SegmentCustomersResponse>>>
      getCustomersByMultipleSegmentTags(
          @AuthenticationPrincipal CustomUser user,
          @Parameter(description = "세그먼트 태그 목록 (쉼표로 구분, 예: NEW,GROWING,LOYAL)", required = true)
              @RequestParam
              List<String> segmentTags) {

    log.info("다중 세그먼트별 고객 조회 요청: {}, 매장ID: {}", segmentTags, user.getShopId());

    // 태그들을 대문자로 변환
    List<String> upperCaseTags = segmentTags.stream().map(String::toUpperCase).toList();

    List<SegmentCustomersResponse> responses =
        customerQueryService.getCustomersByMultipleSegmentTags(upperCaseTags, user.getShopId());

    int totalCustomers = responses.stream().mapToInt(SegmentCustomersResponse::customerCount).sum();

    log.info(
        "다중 세그먼트 조회 완료: {} 개 세그먼트, 총 {} 명의 고객 (매장 {})",
        responses.size(),
        totalCustomers,
        user.getShopId());

    return ResponseEntity.ok(ApiResponse.success(responses));
  }

  @Operation(
      summary = "라이프사이클 세그먼트별 고객 조회",
      description = "모든 라이프사이클 세그먼트(NEW, GROWING, LOYAL, VIP, DORMANT 등)의 고객 ID 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "라이프사이클 세그먼트별 고객 조회 성공",
        content = @Content(schema = @Schema(implementation = SegmentCustomersResponse.class)))
  })
  @GetMapping("/lifecycle/customers")
  public ResponseEntity<ApiResponse<List<SegmentCustomersResponse>>>
      getCustomersByLifecycleSegments(@AuthenticationPrincipal CustomUser user) {

    log.info("라이프사이클 세그먼트별 고객 조회 요청, 매장ID: {}", user.getShopId());

    List<String> lifecycleSegmentTags =
        List.of(
            "NEW",
            "GROWING",
            "LOYAL",
            "VIP",
            "DORMANT",
            "NEW_FOLLOWUP",
            "NEW_AT_RISK",
            "REACTIVATION_NEEDED",
            "GROWING_DELAYED",
            "LOYAL_DELAYED");

    List<SegmentCustomersResponse> responses =
        customerQueryService.getCustomersByMultipleSegmentTags(
            lifecycleSegmentTags, user.getShopId());

    int totalCustomers = responses.stream().mapToInt(SegmentCustomersResponse::customerCount).sum();

    log.info(
        "라이프사이클 세그먼트 조회 완료: {} 개 세그먼트, 총 {} 명의 고객 (매장 {})",
        responses.size(),
        totalCustomers,
        user.getShopId());

    return ResponseEntity.ok(ApiResponse.success(responses));
  }
}
