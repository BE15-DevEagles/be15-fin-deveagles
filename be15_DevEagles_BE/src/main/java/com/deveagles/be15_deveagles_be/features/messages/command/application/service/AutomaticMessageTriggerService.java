package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.AutomaticCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.AutomaticTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import java.util.List;
import java.util.Map;

public interface AutomaticMessageTriggerService {
  void registerAutomaticMessage(Long shopId, AutomaticCreateRequest request);

  void triggerAutomaticSend(
      CustomerDetailResponse customerDto,
      AutomaticEventType eventType,
      Map<String, String> payload);

  List<AutomaticTemplateResponse> getAutomaticMessages(Long shopId);
}
