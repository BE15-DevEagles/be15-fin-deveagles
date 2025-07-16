package com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutomaticTemplateResponse {

  private Long id; // 템플릿 ID
  private String templateName; // 템플릿 이름
  private String templateContent; // 내용
  private Boolean isActive; // 활성화 여부
  private AutomaticEventType automaticEventType; // 자동발송 이벤트 타입

  public static AutomaticTemplateResponse from(MessageTemplate template) {
    return AutomaticTemplateResponse.builder()
        .id(template.getTemplateId())
        .templateName(template.getTemplateName())
        .templateContent(template.getTemplateContent())
        .isActive(template.isActive())
        .automaticEventType(template.getAutomaticEventType())
        .build();
  }
}
