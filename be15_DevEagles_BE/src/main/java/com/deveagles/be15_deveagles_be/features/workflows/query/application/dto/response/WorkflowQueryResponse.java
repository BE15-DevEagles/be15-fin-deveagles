package com.deveagles.be15_deveagles_be.features.workflows.query.application.dto.response;

import com.deveagles.be15_deveagles_be.features.workflows.command.domain.aggregate.Workflow;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowQueryResponse {

  private Long id;
  private String title;
  private String description;
  private Long shopId;
  private Long staffId;
  private Boolean isActive;

  // 대상 고객 조건
  private List<String> targetCustomerGrades;
  private List<String> targetTags;
  private Boolean excludeDormantCustomers;
  private Integer dormantPeriodMonths;
  private Boolean excludeRecentMessageReceivers;
  private Integer recentMessagePeriodDays;

  // 트리거 설정
  private String triggerType;
  private String triggerCategory;
  private Object triggerConfig;

  // 액션 설정
  private String actionType;
  private Object actionConfig;

  // 실행 통계
  private Long executionCount;
  private Long successCount;
  private Long failureCount;
  private Double successRate;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lastExecutedAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime nextScheduledAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime modifiedAt;

  public static WorkflowQueryResponse from(Workflow workflow) {
    if (workflow == null) {
      throw new IllegalArgumentException("Workflow cannot be null");
    }

    ObjectMapper objectMapper = new ObjectMapper();

    try {

      // Calculate success rate safely
      Double successRate = calculateSuccessRate(workflow);

      return WorkflowQueryResponse.builder()
          .id(workflow.getId())
          .title(workflow.getTitle())
          .description(workflow.getDescription())
          .shopId(workflow.getShopId())
          .staffId(workflow.getStaffId())
          .isActive(workflow.getIsActive() != null ? workflow.getIsActive() : false)
          .targetCustomerGrades(parseJsonToList(workflow.getTargetCustomerGrades(), objectMapper))
          .targetTags(parseJsonToList(workflow.getTargetTags(), objectMapper))
          .excludeDormantCustomers(
              workflow.getExcludeDormantCustomers() != null
                  ? workflow.getExcludeDormantCustomers()
                  : false)
          .dormantPeriodMonths(
              workflow.getDormantPeriodMonths() != null ? workflow.getDormantPeriodMonths() : 6)
          .excludeRecentMessageReceivers(
              workflow.getExcludeRecentMessageReceivers() != null
                  ? workflow.getExcludeRecentMessageReceivers()
                  : false)
          .recentMessagePeriodDays(
              workflow.getRecentMessagePeriodDays() != null
                  ? workflow.getRecentMessagePeriodDays()
                  : 30)
          .triggerType(workflow.getTriggerType())
          .triggerCategory(workflow.getTriggerCategory())
          .triggerConfig(parseJsonToObject(workflow.getTriggerConfig(), objectMapper))
          .actionType(workflow.getActionType())
          .actionConfig(parseJsonToObject(workflow.getActionConfig(), objectMapper))
          .executionCount(workflow.getExecutionCount() != null ? workflow.getExecutionCount() : 0L)
          .successCount(workflow.getSuccessCount() != null ? workflow.getSuccessCount() : 0L)
          .failureCount(workflow.getFailureCount() != null ? workflow.getFailureCount() : 0L)
          .successRate(successRate)
          .lastExecutedAt(workflow.getLastExecutedAt())
          .nextScheduledAt(workflow.getNextScheduledAt())
          .createdAt(workflow.getCreatedAt())
          .modifiedAt(workflow.getModifiedAt())
          .build();
    } catch (Exception e) {
      throw new RuntimeException(
          "Error converting Workflow to WorkflowQueryResponse for workflow ID: "
              + workflow.getId()
              + ". Error: "
              + e.getMessage(),
          e);
    }
  }

  private static Double calculateSuccessRate(Workflow workflow) {
    try {
      Long executionCount = workflow.getExecutionCount();
      Long successCount = workflow.getSuccessCount();

      if (executionCount == null || executionCount == 0) {
        return 0.0;
      }

      if (successCount == null) {
        return 0.0;
      }

      return (double) successCount / executionCount * 100;
    } catch (Exception e) {
      // Log the error but don't fail the entire conversion
      return 0.0;
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> parseJsonToList(String json, ObjectMapper objectMapper) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
      return List.of();
    }
    try {
      // Handle cases where the JSON might be malformed
      String trimmedJson = json.trim();
      if (!trimmedJson.startsWith("[") && !trimmedJson.startsWith("{")) {
        // If it's not a proper JSON array or object, return empty list
        return List.of();
      }
      return objectMapper.readValue(trimmedJson, List.class);
    } catch (JsonProcessingException e) {
      // Log the error for debugging but don't fail
      System.err.println("Failed to parse JSON to List: " + json + ", Error: " + e.getMessage());
      return List.of();
    } catch (Exception e) {
      System.err.println(
          "Unexpected error parsing JSON to List: " + json + ", Error: " + e.getMessage());
      return List.of();
    }
  }

  private static Object parseJsonToObject(String json, ObjectMapper objectMapper) {
    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
      return null;
    }
    try {
      // Handle cases where the JSON might be malformed
      String trimmedJson = json.trim();
      if (!trimmedJson.startsWith("{")
          && !trimmedJson.startsWith("[")
          && !trimmedJson.startsWith("\"")) {
        // If it's not a proper JSON object, array, or string, return null
        return null;
      }
      return objectMapper.readValue(trimmedJson, Object.class);
    } catch (JsonProcessingException e) {
      // Log the error for debugging but don't fail
      System.err.println("Failed to parse JSON to Object: " + json + ", Error: " + e.getMessage());
      return null;
    } catch (Exception e) {
      System.err.println(
          "Unexpected error parsing JSON to Object: " + json + ", Error: " + e.getMessage());
      return null;
    }
  }
}
