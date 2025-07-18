package com.deveagles.be15_deveagles_be.features.workflows.command.infrastructure.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.CreateWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.DeleteWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.ToggleWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.UpdateWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.service.WorkflowCommandService;
import com.deveagles.be15_deveagles_be.features.workflows.command.domain.aggregate.Workflow;
import com.deveagles.be15_deveagles_be.features.workflows.command.domain.repository.WorkflowRepository;
import com.deveagles.be15_deveagles_be.features.workflows.command.domain.vo.ActionType;
import com.deveagles.be15_deveagles_be.features.workflows.command.domain.vo.TriggerType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkflowCommandServiceImpl implements WorkflowCommandService {

  private final WorkflowRepository workflowRepository;
  private final ObjectMapper objectMapper;

  @Override
  public Long createWorkflow(CreateWorkflowCommand command) {
    log.info("워크플로우 생성 시작: 제목={}, 매장ID={}", command.getTitle(), command.getShopId());

    validateWorkflowTitleNotExists(command.getTitle(), command.getShopId());

    Workflow workflow = buildWorkflowFromCommand(command);
    Workflow savedWorkflow = workflowRepository.save(workflow);

    log.info("워크플로우 생성 완료: ID={}, 제목={}", savedWorkflow.getId(), savedWorkflow.getTitle());
    return savedWorkflow.getId();
  }

  @Override
  public void updateWorkflow(UpdateWorkflowCommand command) {
    log.info("워크플로우 수정 시작: ID={}, 제목={}", command.getWorkflowId(), command.getTitle());

    // Validate command data before processing
    validateUpdateCommand(command);

    Workflow workflow = findWorkflowByIdAndShopId(command.getWorkflowId(), command.getShopId());

    validateWorkflowOwnership(workflow, command.getStaffId());

    validateWorkflowTitleNotExistsForUpdate(
        command.getTitle(), command.getShopId(), command.getWorkflowId());

    updateWorkflowFromCommand(workflow, command);
    workflowRepository.save(workflow);

    log.info("워크플로우 수정 완료: ID={}, 새 제목={}", workflow.getId(), workflow.getTitle());
  }

  @Override
  public void deleteWorkflow(DeleteWorkflowCommand command) {
    log.info("워크플로우 삭제 시작: ID={}", command.getWorkflowId());

    Workflow workflow = findWorkflowByIdAndShopId(command.getWorkflowId(), command.getShopId());

    validateWorkflowOwnership(workflow, command.getStaffId());

    workflow.softDelete();
    workflowRepository.save(workflow);

    log.info("워크플로우 삭제 완료: ID={}", workflow.getId());
  }

  @Override
  public void toggleWorkflowStatus(ToggleWorkflowCommand command) {
    log.info("워크플로우 상태 토글 시작: ID={}", command.getWorkflowId());

    Workflow workflow = findWorkflowByIdAndShopId(command.getWorkflowId(), command.getShopId());

    validateWorkflowOwnership(workflow, command.getStaffId());

    workflow.toggleStatus();
    workflowRepository.save(workflow);

    log.info("워크플로우 상태 토글 완료: ID={}, 새 상태={}", workflow.getId(), workflow.getIsActive());
  }

  private void validateWorkflowTitleNotExists(String title, Long shopId) {
    if (workflowRepository.existsByTitleAndShopId(title, shopId)) {
      throw new BusinessException(ErrorCode.WORKFLOW_TITLE_ALREADY_EXISTS);
    }
  }

  private void validateWorkflowTitleNotExistsForUpdate(String title, Long shopId, Long excludeId) {
    if (workflowRepository.existsByTitleAndShopIdAndIdNot(title, shopId, excludeId)) {
      throw new BusinessException(ErrorCode.WORKFLOW_TITLE_ALREADY_EXISTS);
    }
  }

  private Workflow findWorkflowByIdAndShopId(Long workflowId, Long shopId) {
    return workflowRepository
        .findByIdAndShopId(workflowId, shopId)
        .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND));
  }

  private void validateWorkflowOwnership(Workflow workflow, Long staffId) {
    if (!workflow.isOwnedByStaff(staffId)) {
      throw new BusinessException(ErrorCode.WORKFLOW_ACCESS_DENIED);
    }
  }

  private Workflow buildWorkflowFromCommand(CreateWorkflowCommand command) {
    return Workflow.builder()
        .title(command.getTitle())
        .description(command.getDescription())
        .shopId(command.getShopId())
        .staffId(command.getStaffId())
        .isActive(command.getIsActive())
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
        .build();
  }

  private void updateWorkflowFromCommand(Workflow workflow, UpdateWorkflowCommand command) {
    workflow.updateBasicInfo(command.getTitle(), command.getDescription(), command.getIsActive());

    workflow.updateTargetConditions(
        command.getTargetCustomerGrades(),
        command.getTargetTags(),
        command.getExcludeDormantCustomers(),
        command.getDormantPeriodMonths(),
        command.getExcludeRecentMessageReceivers(),
        command.getRecentMessagePeriodDays());

    workflow.updateTrigger(
        command.getTriggerType(), command.getTriggerCategory(), command.getTriggerConfig());

    workflow.updateAction(command.getActionType(), command.getActionConfig());
  }

  private void validateUpdateCommand(UpdateWorkflowCommand command) {
    log.debug("Validating update command for workflow ID: {}", command.getWorkflowId());

    // Validate trigger type
    if (command.getTriggerType() != null && !command.getTriggerType().trim().isEmpty()) {
      try {
        TriggerType.fromCode(command.getTriggerType());
      } catch (IllegalArgumentException e) {
        log.error("Invalid trigger type: {}", command.getTriggerType());
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
    }

    // Validate action type
    if (command.getActionType() != null && !command.getActionType().trim().isEmpty()) {
      try {
        ActionType.fromCode(command.getActionType());
      } catch (IllegalArgumentException e) {
        log.error("Invalid action type: {}", command.getActionType());
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
    }

    // Validate JSON fields
    validateJsonField(command.getTargetCustomerGrades(), "targetCustomerGrades");
    validateJsonField(command.getTargetTags(), "targetTags");
    validateJsonField(command.getTriggerConfig(), "triggerConfig");
    validateJsonField(command.getActionConfig(), "actionConfig");

    log.debug("Update command validation completed successfully");
  }

  private void validateJsonField(String jsonField, String fieldName) {
    if (jsonField != null && !jsonField.trim().isEmpty()) {
      try {
        objectMapper.readTree(jsonField);
      } catch (Exception e) {
        log.error("Invalid JSON format in field {}: {}", fieldName, e.getMessage());
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
    }
  }
}
