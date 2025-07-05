package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateUpdateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;

public interface MessageTemplateService {
  MessageTemplateResponse createTemplate(
      Long shopId, MessageTemplateCreateRequest templateCreateRequest);

  void changeTemplate(Long templateId, Long shopId, MessageTemplateUpdateRequest request);

  void removeTemplate(Long templateId, Long shopId);
}
