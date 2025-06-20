package com.deveagles.be15_deveagles_be.features.customers.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerSearchResult;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "고객 검색 관리", description = "엘라스틱서치 인덱스 관리 및 동기화 API")
@RestController
@RequestMapping("/customers/elasticsearch")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CustomerElasticsearchController {

  private final CustomerQueryService customerQueryService;

  @Operation(summary = "개별 고객 엘라스틱서치 동기화", description = "특정 고객의 데이터를 엘라스틱서치에 동기화합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "동기화 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "고객을 찾을 수 없음")
  })
  @PostMapping("/sync/{customerId}")
  public ResponseEntity<ApiResponse<String>> syncCustomer(
      @Parameter(description = "고객 ID", required = true, example = "1") @PathVariable
          Long customerId) {
    log.info("고객 엘라스틱서치 동기화 요청 - 고객ID: {}", customerId);

    try {
      customerQueryService.syncCustomerToElasticsearch(customerId);
      return ResponseEntity.ok(ApiResponse.success("고객 데이터가 성공적으로 동기화되었습니다."));
    } catch (Exception e) {
      log.error("고객 동기화 실패 - 고객ID: {}, 오류: {}", customerId, e.getMessage());
      return ResponseEntity.ok(ApiResponse.success("고객 동기화 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  @Operation(summary = "매장별 전체 고객 재인덱싱", description = "특정 매장의 모든 고객 데이터를 엘라스틱서치에 재인덱싱합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "재인덱싱 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 매장 ID")
  })
  @PostMapping("/reindex")
  public ResponseEntity<ApiResponse<String>> reindexCustomers(
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("매장별 고객 재인덱싱 요청 - 매장ID: {}", shopId);

    try {
      customerQueryService.reindexAllCustomers(shopId);
      return ResponseEntity.ok(ApiResponse.success("매장의 모든 고객 데이터가 성공적으로 재인덱싱되었습니다."));
    } catch (Exception e) {
      log.error("매장 재인덱싱 실패 - 매장ID: {}, 오류: {}", shopId, e.getMessage());
      return ResponseEntity.ok(ApiResponse.success("재인덱싱 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  @Operation(summary = "자동완성 검색", description = "고객명/전화번호 자동완성을 위한 검색 결과를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "자동완성 검색 성공")
  })
  @GetMapping("/autocomplete")
  public ResponseEntity<ApiResponse<List<String>>> autocomplete(
      @Parameter(description = "검색 키워드", required = true, example = "홍") @RequestParam
          String prefix,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("자동완성 검색 요청 - 키워드: {}, 매장ID: {}", prefix, shopId);

    try {
      List<String> suggestions = customerQueryService.autocomplete(prefix, shopId);
      return ResponseEntity.ok(ApiResponse.success(suggestions));
    } catch (Exception e) {
      log.error("자동완성 검색 실패 - 키워드: {}, 매장ID: {}, 오류: {}", prefix, shopId, e.getMessage());
      return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
  }

  @Operation(summary = "검색 결과 개수", description = "키워드로 검색되는 고객 수를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "검색 개수 조회 성공")
  })
  @GetMapping("/count")
  public ResponseEntity<ApiResponse<Long>> countByKeyword(
      @Parameter(description = "검색 키워드", required = true, example = "홍길동") @RequestParam
          String keyword,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("키워드별 고객 수 조회 요청 - 키워드: {}, 매장ID: {}", keyword, shopId);

    try {
      long count = customerQueryService.countByKeyword(keyword, shopId);
      return ResponseEntity.ok(ApiResponse.success(count));
    } catch (Exception e) {
      log.error("키워드별 고객 수 조회 실패 - 키워드: {}, 매장ID: {}, 오류: {}", keyword, shopId, e.getMessage());
      return ResponseEntity.ok(ApiResponse.success(0L));
    }
  }

  @Operation(summary = "키워드 검색", description = "간단한 키워드로 고객을 검색합니다 (페이징 없음).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "키워드 검색 성공")
  })
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<CustomerSearchResult>>> searchByKeyword(
      @Parameter(description = "검색 키워드", required = true, example = "홍길동") @RequestParam
          String keyword,
      @Parameter(description = "매장 ID", required = true, example = "1") @RequestParam Long shopId) {
    log.info("키워드 검색 요청 - 키워드: {}, 매장ID: {}", keyword, shopId);

    try {
      List<CustomerSearchResult> results = customerQueryService.searchByKeyword(keyword, shopId);
      return ResponseEntity.ok(ApiResponse.success(results));
    } catch (Exception e) {
      log.error("키워드 검색 실패 - 키워드: {}, 매장ID: {}, 오류: {}", keyword, shopId, e.getMessage());
      return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
  }

  @Operation(summary = "엘라스틱서치 헬스체크", description = "엘라스틱서치 연결 상태를 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "헬스체크 완료")
  })
  @GetMapping("/health")
  public ResponseEntity<ApiResponse<String>> healthCheck() {
    log.info("엘라스틱서치 헬스체크 요청");

    try {
      customerQueryService.autocomplete("test", 1L);
      return ResponseEntity.ok(ApiResponse.success("엘라스틱서치가 정상적으로 작동 중입니다."));
    } catch (Exception e) {
      log.error("엘라스틱서치 헬스체크 실패: {}", e.getMessage());
      return ResponseEntity.ok(ApiResponse.success("엘라스틱서치 연결에 문제가 있습니다: " + e.getMessage()));
    }
  }
}
