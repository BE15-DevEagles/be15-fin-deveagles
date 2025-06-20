package com.deveagles.be15_deveagles_be.features.customers.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.request.CustomerSearchQuery;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerListResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerSearchResult;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "고객 조회", description = "고객 조회 및 검색 API")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CustomerQueryController {

  private final CustomerQueryService customerQueryService;

  @Operation(
      summary = "고객 상세 조회",
      description = "고객 ID로 고객의 상세 정보를 조회합니다. 등급명, 유입경로명, 태그 정보 등을 포함합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 조회 성공",
        content = @Content(schema = @Schema(implementation = CustomerDetailResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @GetMapping("/{customerId}")
  public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(
      @Parameter(description = "고객 ID", required = true, example = "1") @PathVariable
          Long customerId,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("고객 상세 조회 요청 - 고객ID: {}, 매장ID: {}", customerId, shopId);

    CustomerDetailResponse response =
        customerQueryService
            .getCustomerDetail(customerId, shopId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "고객을 찾을 수 없습니다."));
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "전화번호로 고객 조회", description = "전화번호로 고객을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 조회 성공",
        content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @GetMapping("/phone/{phoneNumber}")
  public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByPhoneNumber(
      @Parameter(description = "전화번호", required = true, example = "01012345678") @PathVariable
          String phoneNumber,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("전화번호로 고객 조회 요청 - 전화번호: {}, 매장ID: {}", phoneNumber, shopId);

    CustomerResponse customer =
        customerQueryService
            .getCustomerByPhoneNumber(phoneNumber, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    return ResponseEntity.ok(ApiResponse.success(customer));
  }

  @Operation(
      summary = "매장별 고객 목록 조회",
      description =
          "매장 ID로 상세한 고객 목록을 조회합니다. 고객명, 연락처, 담당자, 메모, 방문횟수, 실매출액, 최근방문일, 태그, 등급, 생일 정보를 포함합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 파라미터")
  })
  @GetMapping("/shop/{shopId}")
  public ResponseEntity<ApiResponse<List<CustomerListResponse>>> getCustomersByShopId(
      @Parameter(description = "매장 ID", required = true, example = "1") @PathVariable Long shopId) {
    log.info("매장별 고객 목록 조회 요청 - 매장ID: {}", shopId);

    List<CustomerListResponse> customers = customerQueryService.getCustomerList(shopId);
    return ResponseEntity.ok(ApiResponse.success(customers));
  }

  @Operation(summary = "매장별 고객 목록 조회 (페이징)", description = "매장 ID로 상세한 고객 목록을 페이징으로 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 목록 조회 성공")
  })
  @GetMapping("/shop/{shopId}/paged")
  public ResponseEntity<ApiResponse<PagedResponse<CustomerListResponse>>> getCustomersByShopIdPaged(
      @Parameter(description = "매장 ID", required = true, example = "1") @PathVariable Long shopId,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20")
          int size) {
    log.info("매장별 고객 목록 페이징 조회 요청 - 매장ID: {}, 페이지: {}, 크기: {}", shopId, page, size);

    Pageable pageable = Pageable.ofSize(size).withPage(page);
    Page<CustomerListResponse> customerPage =
        customerQueryService.getCustomerListPaged(shopId, pageable);
    PagedResponse<CustomerListResponse> response =
        PagedResponse.from(PagedResult.from(customerPage));
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "고객 통합 검색", description = "다양한 조건으로 고객을 검색하고 페이징 결과를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 검색 성공",
        content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  })
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<PagedResponse<CustomerSearchResult>>> searchCustomers(
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId,
      @Parameter(description = "검색 키워드 (이름 또는 전화번호)", example = "홍길동")
          @RequestParam(required = false)
          String keyword,
      @Parameter(description = "고객 등급 ID", example = "1") @RequestParam(required = false)
          Long customerGradeId,
      @Parameter(description = "성별 (M/F)", example = "M") @RequestParam(required = false)
          String gender,
      @Parameter(description = "마케팅 동의 여부", example = "true") @RequestParam(required = false)
          Boolean marketingConsent,
      @Parameter(description = "알림 동의 여부", example = "true") @RequestParam(required = false)
          Boolean notificationConsent,
      @Parameter(description = "삭제된 고객 포함 여부", example = "false")
          @RequestParam(required = false, defaultValue = "false")
          Boolean includeDeleted,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20")
          int size,
      @Parameter(description = "정렬 기준 필드", example = "createdAt")
          @RequestParam(defaultValue = "createdAt")
          String sortBy,
      @Parameter(description = "정렬 방향 (ASC/DESC)", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          String sortDirection) {

    log.info(
        "고객 통합 검색 요청 - 매장ID: {}, 키워드: {}, 등급ID: {}, 성별: {}",
        shopId,
        keyword,
        customerGradeId,
        gender);

    CustomerSearchQuery query =
        new CustomerSearchQuery(
            shopId,
            keyword,
            customerGradeId,
            gender,
            marketingConsent,
            notificationConsent,
            includeDeleted,
            page,
            size,
            sortBy,
            sortDirection);

    PagedResult<CustomerSearchResult> pagedResult = customerQueryService.advancedSearch(query);
    PagedResponse<CustomerSearchResult> response = PagedResponse.from(pagedResult);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "매장별 고객 수 조회", description = "매장의 전체 고객 수를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "고객 수 조회 성공")
  })
  @GetMapping("/count")
  public ResponseEntity<ApiResponse<Long>> getCustomerCountByShopId(
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("매장별 고객 수 조회 요청 - 매장ID: {}", shopId);

    long count = customerQueryService.getCustomerCountByShopId(shopId);
    return ResponseEntity.ok(ApiResponse.success(count));
  }

  @Operation(summary = "전화번호 중복 확인", description = "전화번호의 중복 여부를 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "중복 확인 성공")
  })
  @GetMapping("/exists")
  public ResponseEntity<ApiResponse<Boolean>> checkPhoneNumberExists(
      @Parameter(description = "전화번호", required = true, example = "01012345678") @RequestParam
          String phoneNumber,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("전화번호 중복 확인 요청 - 전화번호: {}, 매장ID: {}", phoneNumber, shopId);

    boolean exists = customerQueryService.existsByPhoneNumber(phoneNumber, shopId);
    return ResponseEntity.ok(ApiResponse.success(exists));
  }
}
