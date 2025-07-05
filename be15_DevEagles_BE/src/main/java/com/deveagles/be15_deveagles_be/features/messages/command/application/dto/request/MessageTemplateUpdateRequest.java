package com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request;

import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplateUpdateRequest {

  @NotBlank private String templateName;

  @NotBlank private String templateContent;

  @NotNull private MessageTemplateType templateType;

  private Long customerGradeId;
  private Long tagId;
}
