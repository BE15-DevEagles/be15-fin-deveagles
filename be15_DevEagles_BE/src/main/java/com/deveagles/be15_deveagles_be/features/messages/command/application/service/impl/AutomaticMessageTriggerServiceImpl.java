package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.AutomaticCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.AutomaticTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageCommandService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageVariableProcessor;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplateType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageTemplateRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutomaticMessageTriggerServiceImpl implements AutomaticMessageTriggerService {
  private final MessageTemplateRepository messageTemplateRepository;
  private final MessageTemplateQueryRepository messageTemplateQueryRepository;
  private final MessageCommandService messageCommandService;
  private final MessageVariableProcessor messageVariableProcessor;

  @Override
  public void registerAutomaticMessage(Long shopId, AutomaticCreateRequest request) {
    AutomaticEventType eventType = request.automaticEventType();

    // 동일 이벤트 타입으로 이미 등록된 템플릿 있는지 확인
    if (messageTemplateRepository.existsByShopIdAndAutomaticEventType(shopId, eventType)) {
      throw new BusinessException(ErrorCode.AUTOMATIC_TEMPLATE_ALREADY_EXISTS);
    }

    // 템플릿 이름 자동 설정
    String templateName =
        switch (eventType) {
          case NEW_CUSTOMER -> "신규 고객 자동 메시지";
          case RESERVATION_CREATED -> "예약 등록 자동 메시지";
          case RESERVATION_MODIFIED -> "예약 수정 자동 메시지";
          case RESERVATION_CANCELLED -> "예약 취소 자동 메시지";
          case SESSION_PASS_USED -> "횟수권 차감 자동 메시지";
          case PREPAID_USED -> "선불권 차감 자동 메시지";
        };

    // 템플릿 생성
    MessageTemplate template =
        MessageTemplate.builder()
            .shopId(shopId)
            .templateName(templateName)
            .templateContent(request.templateContent())
            .templateType(MessageTemplateType.announcement)
            .automaticEventType(eventType)
            .isActive(request.isActive())
            .build();

    messageTemplateRepository.save(template);
  }

  @Override
  public List<AutomaticTemplateResponse> getAutomaticMessages(Long shopId) {
    return messageTemplateRepository.findByShopIdAndAutomaticEventTypeIsNotNull(shopId).stream()
        .map(AutomaticTemplateResponse::from) // 정확히 일치
        .toList();
  }

  @Override
  public void triggerAutomaticSend(
      CustomerDetailResponse customerDto,
      AutomaticEventType eventType,
      Map<String, String> payload) {
    Optional<MessageTemplate> templateOpt =
        messageTemplateQueryRepository.findActiveTemplate(customerDto.getShopId(), eventType);

    if (templateOpt.isEmpty()) return;

    MessageTemplate template = templateOpt.get();

    // buildPayload는 고객정보 + 추가 payload 병합해서 하나로 처리
    Map<String, String> mergedPayload =
        messageVariableProcessor.buildPayload(
            customerDto.getCustomerId(), customerDto.getShopId(), payload);

    SmsRequest request = SmsRequest.ofForAutoSend(template, customerDto, mergedPayload);

    messageCommandService.sendSms(customerDto.getShopId(), request);
  }
}
