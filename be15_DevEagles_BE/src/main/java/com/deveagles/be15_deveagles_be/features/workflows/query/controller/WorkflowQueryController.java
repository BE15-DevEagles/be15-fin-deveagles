package com.deveagles.be15_deveagles_be.features.workflows.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResponse;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.workflows.query.application.dto.request.WorkflowSearchRequest;
import com.deveagles.be15_deveagles_be.features.workflows.query.application.dto.response.WorkflowQueryResponse;
import com.deveagles.be15_deveagles_be.features.workflows.query.application.dto.response.WorkflowStatsResponse;
import com.deveagles.be15_deveagles_be.features.workflows.query.application.dto.response.WorkflowSummaryResponse;
import com.deveagles.be15_deveagles_be.features.workflows.query.application.service.WorkflowQueryService;
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

@Tag(name = "워크플로우 조회", description = "워크플로우 조회 및 검색 API")
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@Validated
@Slf4j
public class WorkflowQueryController {

  private final WorkflowQueryService workflowQueryService;

  @Operation(summary = "워크플로우 상세 조회", description = "워크플로우 ID로 상세 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크플로우 조회 성공",
        content = @Content(schema = @Schema(implementation = WorkflowQueryResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "워크플로우를 찾을 수 없음")
  })
  @GetMapping("/{workflowId}")
  public ResponseEntity<ApiResponse<WorkflowQueryResponse>> getWorkflow(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "워크플로우 ID", required = true) @PathVariable Long workflowId) {

    if (user == null) {
      log.error("인증되지 않은 사용자의 워크플로우 조회 요청 - workflowId: {}", workflowId);
      return ResponseEntity.status(ErrorCode.AUTHENTICATION_FAILED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.AUTHENTICATION_FAILED.getCode(),
                  ErrorCode.AUTHENTICATION_FAILED.getMessage()));
    }

    log.info(
        "워크플로우 상세 조회 요청 - workflowId: {}, shopId: {}, userId: {}",
        workflowId,
        user.getShopId(),
        user.getUserId());

    if (workflowId == null || workflowId <= 0) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "워크플로우 ID는 양수여야 합니다"));
    }

    try {
      WorkflowQueryResponse response =
          workflowQueryService.getWorkflowById(workflowId, user.getShopId());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("워크플로우 상세 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 상세 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "워크플로우 목록 검색", description = "다양한 조건으로 워크플로우를 검색하고 페이징 결과를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크플로우 검색 성공")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<WorkflowSummaryResponse>>> searchWorkflows(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "검색어 (제목, 설명)") @RequestParam(required = false) String searchQuery,
      @Parameter(description = "상태 필터 (active, inactive, all)") @RequestParam(required = false)
          String statusFilter,
      @Parameter(description = "트리거 카테고리") @RequestParam(required = false) String triggerCategory,
      @Parameter(description = "트리거 타입") @RequestParam(required = false) String triggerType,
      @Parameter(description = "액션 타입") @RequestParam(required = false) String actionType,
      @Parameter(description = "활성화 상태") @RequestParam(required = false) Boolean isActive,
      @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sortBy,
      @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String sortDirection) {

    log.info("워크플로우 목록 검색 요청 - shopId: {}, page: {}, size: {}", user.getShopId(), page, size);

    // 페이지 파라미터 검증
    if (page < 0 || size <= 0 || size > 100) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_PAGE_PARAMETER.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_PAGE_PARAMETER.getCode(),
                  "페이지 번호는 0 이상, 크기는 1-100 사이여야 합니다"));
    }

    // 상태 필터 검증
    if (statusFilter != null && !statusFilter.matches("^(active|inactive|all)$")) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_STATUS_FILTER.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_STATUS_FILTER.getCode(),
                  "상태 필터는 active, inactive, all 중 하나여야 합니다"));
    }

    // 정렬 파라미터 검증
    if (!sortBy.matches("^(createdAt|modifiedAt|title|executionCount|successRate)$")) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_SORT_PARAMETER.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_SORT_PARAMETER.getCode(), "정렬 기준이 유효하지 않습니다"));
    }

    if (!sortDirection.matches("^(asc|desc)$")) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_SORT_PARAMETER.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_SORT_PARAMETER.getCode(), "정렬 방향은 asc 또는 desc여야 합니다"));
    }

    try {
      WorkflowSearchRequest request =
          WorkflowSearchRequest.builder()
              .shopId(user.getShopId())
              .searchQuery(searchQuery)
              .statusFilter(statusFilter)
              .triggerCategory(triggerCategory)
              .triggerType(triggerType)
              .actionType(actionType)
              .isActive(isActive)
              .page(page)
              .size(size)
              .sortBy(sortBy)
              .sortDirection(sortDirection)
              .build();

      PagedResponse<WorkflowSummaryResponse> response =
          workflowQueryService.searchWorkflows(request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("워크플로우 목록 검색 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 목록 검색 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.WORKFLOW_SEARCH_FAILED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_SEARCH_FAILED.getCode(),
                  ErrorCode.WORKFLOW_SEARCH_FAILED.getMessage()));
    }
  }

  @Operation(summary = "워크플로우 통계 조회", description = "매장의 워크플로우 관련 통계 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "통계 조회 성공",
        content = @Content(schema = @Schema(implementation = WorkflowStatsResponse.class)))
  })
  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<WorkflowStatsResponse>> getWorkflowStats(
      @AuthenticationPrincipal CustomUser user) {

    log.info("워크플로우 통계 조회 요청 - shopId: {}", user.getShopId());

    try {
      WorkflowStatsResponse response = workflowQueryService.getWorkflowStats(user.getShopId());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("워크플로우 통계 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 통계 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.WORKFLOW_STATS_CALCULATION_FAILED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_STATS_CALCULATION_FAILED.getCode(),
                  ErrorCode.WORKFLOW_STATS_CALCULATION_FAILED.getMessage()));
    }
  }

  @Operation(summary = "트리거 카테고리별 워크플로우 조회", description = "특정 트리거 카테고리에 속하는 워크플로우 목록을 조회합니다.")
  @GetMapping("/trigger-category/{triggerCategory}")
  public ResponseEntity<ApiResponse<List<WorkflowSummaryResponse>>> getWorkflowsByTriggerCategory(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "트리거 카테고리", required = true) @PathVariable String triggerCategory) {

    log.info(
        "트리거 카테고리별 워크플로우 조회 요청 - triggerCategory: {}, shopId: {}",
        triggerCategory,
        user.getShopId());

    if (triggerCategory == null || triggerCategory.trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(ApiResponse.failure(ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "트리거 카테고리는 필수입니다"));
    }

    try {
      List<WorkflowSummaryResponse> response =
          workflowQueryService.getWorkflowsByTriggerCategory(triggerCategory, user.getShopId());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("트리거 카테고리별 워크플로우 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("트리거 카테고리별 워크플로우 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "트리거 타입별 워크플로우 조회", description = "특정 트리거 타입에 속하는 워크플로우 목록을 조회합니다.")
  @GetMapping("/trigger-type/{triggerType}")
  public ResponseEntity<ApiResponse<List<WorkflowSummaryResponse>>> getWorkflowsByTriggerType(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "트리거 타입", required = true) @PathVariable String triggerType) {

    log.info("트리거 타입별 워크플로우 조회 요청 - triggerType: {}, shopId: {}", triggerType, user.getShopId());

    if (triggerType == null || triggerType.trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(ApiResponse.failure(ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "트리거 타입은 필수입니다"));
    }

    try {
      List<WorkflowSummaryResponse> response =
          workflowQueryService.getWorkflowsByTriggerType(triggerType, user.getShopId());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("트리거 타입별 워크플로우 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("트리거 타입별 워크플로우 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "최근 워크플로우 조회", description = "최근 생성된 워크플로우 목록을 조회합니다.")
  @GetMapping("/recent")
  public ResponseEntity<ApiResponse<List<WorkflowSummaryResponse>>> getRecentWorkflows(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "조회할 개수") @RequestParam(defaultValue = "10") int limit) {

    log.info("최근 워크플로우 조회 요청 - shopId: {}, limit: {}", user.getShopId(), limit);

    if (limit <= 0 || limit > 100) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "조회 개수는 1-100 사이여야 합니다"));
    }

    try {
      List<WorkflowSummaryResponse> response =
          workflowQueryService.getRecentWorkflows(user.getShopId(), limit);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("최근 워크플로우 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("최근 워크플로우 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "활성 워크플로우 조회", description = "활성화된 워크플로우 목록을 조회합니다.")
  @GetMapping("/active")
  public ResponseEntity<ApiResponse<List<WorkflowSummaryResponse>>> getActiveWorkflows(
      @AuthenticationPrincipal CustomUser user) {

    log.info("활성 워크플로우 조회 요청 - shopId: {}", user.getShopId());

    try {
      List<WorkflowSummaryResponse> response =
          workflowQueryService.getActiveWorkflows(user.getShopId());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (BusinessException e) {
      log.warn("활성 워크플로우 조회 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("활성 워크플로우 조회 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }
}
