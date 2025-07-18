package com.deveagles.be15_deveagles_be.features.workflows.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Workflow {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workflow_id")
  private Long id;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "shop_id", nullable = false)
  private Long shopId;

  @Column(name = "staff_id", nullable = false)
  private Long staffId;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  // 대상 고객 조건들
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "target_customer_grades", columnDefinition = "json")
  private String targetCustomerGrades;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "target_tags", columnDefinition = "json")
  private String targetTags;

  @Column(name = "exclude_dormant_customers", nullable = false)
  @Builder.Default
  private Boolean excludeDormantCustomers = false;

  @Column(name = "dormant_period_months")
  @Builder.Default
  private Integer dormantPeriodMonths = 6;

  @Column(name = "exclude_recent_message_receivers", nullable = false)
  @Builder.Default
  private Boolean excludeRecentMessageReceivers = false;

  @Column(name = "recent_message_period_days")
  @Builder.Default
  private Integer recentMessagePeriodDays = 30;

  // 트리거 설정
  @Column(name = "trigger_type", nullable = false, length = 50)
  private String triggerType; // visit-cycle, specific-treatment, birthday 등

  @Column(name = "trigger_category", nullable = false, length = 20)
  private String triggerCategory; // lifecycle, periodic, special, prevention

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "trigger_config", columnDefinition = "json")
  private String triggerConfig; // 트리거별 세부 설정 JSON

  // 액션 설정
  @Column(name = "action_type", nullable = false, length = 50)
  private String actionType; // message-only, coupon-message, system-notification

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "action_config", columnDefinition = "json")
  private String actionConfig; // 액션별 세부 설정 JSON

  // 실행 통계
  @Column(name = "execution_count", nullable = false)
  @Builder.Default
  private Long executionCount = 0L;

  @Column(name = "success_count", nullable = false)
  @Builder.Default
  private Long successCount = 0L;

  @Column(name = "failure_count", nullable = false)
  @Builder.Default
  private Long failureCount = 0L;

  @Column(name = "last_executed_at")
  private LocalDateTime lastExecutedAt;

  @Column(name = "next_scheduled_at")
  private LocalDateTime nextScheduledAt;

  // 메타데이터
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "modified_at", nullable = false)
  private LocalDateTime modifiedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  // Business methods
  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void toggleStatus() {
    this.isActive = !this.isActive;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.isActive = false;
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  public boolean canExecute() {
    return isActive && !isDeleted();
  }

  public void recordExecution(boolean success) {
    this.executionCount = (this.executionCount == null ? 0L : this.executionCount) + 1;
    if (success) {
      this.successCount = (this.successCount == null ? 0L : this.successCount) + 1;
    } else {
      this.failureCount = (this.failureCount == null ? 0L : this.failureCount) + 1;
    }
    this.lastExecutedAt = LocalDateTime.now();
  }

  public double getSuccessRate() {
    if (executionCount == 0) {
      return 0.0;
    }
    return (double) successCount / executionCount * 100;
  }

  public void updateSchedule(LocalDateTime nextScheduled) {
    this.nextScheduledAt = nextScheduled;
  }

  public boolean isScheduledToRun() {
    return nextScheduledAt != null && nextScheduledAt.isBefore(LocalDateTime.now());
  }

  public boolean belongsToShop(Long requestShopId) {
    return this.shopId.equals(requestShopId);
  }

  public boolean isOwnedByStaff(Long requestStaffId) {
    return this.staffId.equals(requestStaffId);
  }

  // 워크플로우 업데이트
  public void updateBasicInfo(String title, String description, Boolean isActive) {
    this.title = title;
    this.description = description;
    this.isActive = isActive;
  }

  public void updateTargetConditions(
      String targetCustomerGrades,
      String targetTags,
      Boolean excludeDormantCustomers,
      Integer dormantPeriodMonths,
      Boolean excludeRecentMessageReceivers,
      Integer recentMessagePeriodDays) {
    this.targetCustomerGrades = targetCustomerGrades;
    this.targetTags = targetTags;
    this.excludeDormantCustomers = excludeDormantCustomers;
    this.dormantPeriodMonths = dormantPeriodMonths;
    this.excludeRecentMessageReceivers = excludeRecentMessageReceivers;
    this.recentMessagePeriodDays = recentMessagePeriodDays;
  }

  public void updateTrigger(String triggerType, String triggerCategory, String triggerConfig) {
    this.triggerType = triggerType;
    this.triggerCategory = triggerCategory;
    this.triggerConfig = triggerConfig;
  }

  public void updateAction(String actionType, String actionConfig) {
    this.actionType = actionType;
    this.actionConfig = actionConfig;
  }
}
