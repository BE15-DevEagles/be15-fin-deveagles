package com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplateType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MessageTemplateResponse {

  private Long templateId;
  private String templateName;
  private String templateContent;
  private MessageTemplateType templateType;
  private Long shopId;
  private Long customerGradeId;
  private Long tagId;
  private LocalDateTime createdAt;

  public static MessageTemplateResponse from(MessageTemplate t) {
    return MessageTemplateResponse.builder()
        .templateId(t.getTemplateId())
        .templateName(t.getTemplateName())
        .templateContent(t.getTemplateContent())
        .templateType(t.getTemplateType())
        .shopId(t.getShopId())
        .customerGradeId(t.getCustomerGradeId())
        .tagId(t.getTagId())
        .createdAt(t.getCreatedAt())
        .build();
  }
}
