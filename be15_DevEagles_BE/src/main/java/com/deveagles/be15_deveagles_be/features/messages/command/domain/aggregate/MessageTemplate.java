package com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "message_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessageTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "template_id")
  private Long templateId;

  @Column(name = "template_name", nullable = false, length = 50)
  private String templateName;

  @Column(name = "template_content", nullable = false, length = 500)
  private String templateContent;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "shop_id", nullable = false)
  private Long shopId;

  @Enumerated(EnumType.STRING)
  @Column(name = "template_type", nullable = false, length = 10)
  private MessageTemplateType templateType;

  @Column(name = "customer_grade_id")
  private Long customerGradeId;

  @Column(name = "tag_id")
  private Long tagId;

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isUsableForGrade(Long gradeId) {
    return this.customerGradeId == null || this.customerGradeId.equals(gradeId);
  }

  public boolean isUsableForTag(Long tagId) {
    return this.tagId == null || this.tagId.equals(tagId);
  }

  public boolean isUsableForShop(Long requestShopId) {
    return this.shopId.equals(requestShopId);
  }

  public void update(
      String templateName,
      String templateContent,
      MessageTemplateType type,
      Long gradeId,
      Long tagId) {
    this.templateName = templateName;
    this.templateContent = templateContent;
    this.templateType = type;
    this.customerGradeId = gradeId;
    this.tagId = tagId;
  }
}
