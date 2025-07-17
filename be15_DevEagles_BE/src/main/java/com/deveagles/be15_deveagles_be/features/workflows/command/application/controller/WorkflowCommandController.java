package com.deveagles.be15_deveagles_be.features.workflows.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.CreateWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.DeleteWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.ToggleWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.UpdateWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.service.WorkflowCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "워크플로우 관리", description = "워크플로우 생성, 수정, 삭제 API")
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@Validated
@Slf4j
public class WorkflowCommandController {

  private final WorkflowCommandService workflowCommandService;

  @Operation(summary = "워크플로우 생성", description = "새로운 워크플로우를 생성합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "워크플로우 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 데이터"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복된 워크플로우 제목")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<WorkflowCreateResponse>> createWorkflow(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "워크플로우 생성 정보", required = true) @Valid @RequestBody
          CreateWorkflowCommand command) {
    log.info("워크플로우 생성 요청: 제목={}, 매장ID={}", command.getTitle(), user.getShopId());

    // 입력값 검증
    if (command.getTitle() == null || command.getTitle().trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_TITLE_REQUIRED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_TITLE_REQUIRED.getCode(),
                  ErrorCode.WORKFLOW_TITLE_REQUIRED.getMessage()));
    }

    if (command.getTriggerType() == null || command.getTriggerType().trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_TRIGGER_TYPE_REQUIRED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_TRIGGER_TYPE_REQUIRED.getCode(),
                  ErrorCode.WORKFLOW_TRIGGER_TYPE_REQUIRED.getMessage()));
    }

    if (command.getActionType() == null || command.getActionType().trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_ACTION_TYPE_REQUIRED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_ACTION_TYPE_REQUIRED.getCode(),
                  ErrorCode.WORKFLOW_ACTION_TYPE_REQUIRED.getMessage()));
    }

    try {
      CreateWorkflowCommand updatedCommand =
          CreateWorkflowCommand.builder()
              .title(command.getTitle())
              .description(command.getDescription())
              .shopId(user.getShopId())
              .staffId(user.getUserId())
              .targetCustomerGrades(command.getTargetCustomerGrades())
              .targetTags(command.getTargetTags())
              .excludeDormantCustomers(command.getExcludeDormantCustomers())
              .dormantPeriodMonths(command.getDormantPeriodMonths())
              .excludeRecentMessageReceivers(command.getExcludeRecentMessageReceivers())
              .recentMessagePeriodDays(command.getRecentMessagePeriodDays())
              .triggerType(command.getTriggerType())
              .triggerCategory(command.getTriggerCategory())
              .triggerConfig(command.getTriggerConfig())
              .actionType(command.getActionType())
              .actionConfig(command.getActionConfig())
              .isActive(command.getIsActive())
              .build();

      Long workflowId = workflowCommandService.createWorkflow(updatedCommand);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(new WorkflowCreateResponse(workflowId)));
    } catch (BusinessException e) {
      log.warn("워크플로우 생성 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 생성 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "워크플로우 수정", description = "기존 워크플로우의 정보를 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크플로우 수정 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "워크플로우를 찾을 수 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 데이터"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복된 워크플로우 제목")
  })
  @PutMapping("/{workflowId}")
  public ResponseEntity<ApiResponse<Void>> updateWorkflow(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "워크플로우 ID", required = true, example = "1") @PathVariable
          Long workflowId,
      @Parameter(description = "워크플로우 수정 정보", required = true) @Valid @RequestBody
          UpdateWorkflowCommand command) {
    log.info("워크플로우 수정 요청: ID={}, 제목={}", workflowId, command.getTitle());

    if (workflowId == null || workflowId <= 0) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "워크플로우 ID는 양수여야 합니다"));
    }

    if (command.getTitle() == null || command.getTitle().trim().isEmpty()) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_TITLE_REQUIRED.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_TITLE_REQUIRED.getCode(),
                  ErrorCode.WORKFLOW_TITLE_REQUIRED.getMessage()));
    }

    try {
      UpdateWorkflowCommand commandWithId =
          UpdateWorkflowCommand.builder()
              .workflowId(workflowId)
              .title(command.getTitle())
              .description(command.getDescription())
              .shopId(user.getShopId())
              .staffId(user.getUserId())
              .targetCustomerGrades(command.getTargetCustomerGrades())
              .targetTags(command.getTargetTags())
              .excludeDormantCustomers(command.getExcludeDormantCustomers())
              .dormantPeriodMonths(command.getDormantPeriodMonths())
              .excludeRecentMessageReceivers(command.getExcludeRecentMessageReceivers())
              .recentMessagePeriodDays(command.getRecentMessagePeriodDays())
              .triggerType(command.getTriggerType())
              .triggerCategory(command.getTriggerCategory())
              .triggerConfig(command.getTriggerConfig())
              .actionType(command.getActionType())
              .actionConfig(command.getActionConfig())
              .isActive(command.getIsActive())
              .build();

      workflowCommandService.updateWorkflow(commandWithId);
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (BusinessException e) {
      log.warn("워크플로우 수정 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 수정 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "워크플로우 삭제", description = "기존 워크플로우를 소프트 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크플로우 삭제 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "워크플로우를 찾을 수 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "워크플로우에 대한 접근 권한이 없음")
  })
  @DeleteMapping("/{workflowId}")
  public ResponseEntity<ApiResponse<Void>> deleteWorkflow(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "워크플로우 ID", required = true, example = "1") @PathVariable
          Long workflowId) {
    log.info("워크플로우 삭제 요청: ID={}", workflowId);

    if (workflowId == null || workflowId <= 0) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "워크플로우 ID는 양수여야 합니다"));
    }

    try {
      DeleteWorkflowCommand commandWithId =
          new DeleteWorkflowCommand(workflowId, user.getShopId(), user.getUserId());

      workflowCommandService.deleteWorkflow(commandWithId);
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (BusinessException e) {
      log.warn("워크플로우 삭제 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 삭제 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  @Operation(summary = "워크플로우 상태 토글", description = "워크플로우의 활성/비활성 상태를 토글합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크플로우 상태 토글 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "워크플로우를 찾을 수 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "워크플로우에 대한 접근 권한이 없음")
  })
  @PatchMapping("/{workflowId}/toggle")
  public ResponseEntity<ApiResponse<Void>> toggleWorkflowStatus(
      @AuthenticationPrincipal CustomUser user,
      @Parameter(description = "워크플로우 ID", required = true, example = "1") @PathVariable
          Long workflowId) {
    log.info("워크플로우 상태 토글 요청: ID={}", workflowId);

    if (workflowId == null || workflowId <= 0) {
      return ResponseEntity.status(ErrorCode.WORKFLOW_INVALID_INPUT.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.WORKFLOW_INVALID_INPUT.getCode(), "워크플로우 ID는 양수여야 합니다"));
    }

    try {
      ToggleWorkflowCommand commandWithId =
          new ToggleWorkflowCommand(workflowId, user.getShopId(), user.getUserId());

      workflowCommandService.toggleWorkflowStatus(commandWithId);
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (BusinessException e) {
      log.warn("워크플로우 상태 토글 실패 - 비즈니스 예외: {}", e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus())
          .body(ApiResponse.failure(e.getErrorCode().getCode(), e.getMessage()));
    } catch (Exception e) {
      log.error("워크플로우 상태 토글 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
          .body(
              ApiResponse.failure(
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                  ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
  }

  public static class WorkflowCreateResponse {
    private final Long workflowId;

    public WorkflowCreateResponse(Long workflowId) {
      this.workflowId = workflowId;
    }

    public Long getWorkflowId() {
      return workflowId;
    }
  }
}
