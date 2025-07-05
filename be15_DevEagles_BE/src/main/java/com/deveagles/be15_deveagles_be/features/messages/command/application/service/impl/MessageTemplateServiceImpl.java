package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateUpdateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageTemplateService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageTemplateRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl implements MessageTemplateService {

  private final MessageTemplateRepository messageTemplateRepository;
  private final ShopCommandService shopCommandService;
  private final MessageTemplateQueryRepository messageTemplateQueryRepository;

  @Override
  @Transactional
  public MessageTemplateResponse createTemplate(Long shopId, MessageTemplateCreateRequest request) {
    shopCommandService.validateShopExists(shopId);
    MessageTemplate template =
        MessageTemplate.builder()
            .templateName(request.getTemplateName())
            .templateContent(request.getTemplateContent())
            .templateType(request.getTemplateType())
            .shopId(shopId)
            .customerGradeId(request.getCustomerGradeId())
            .tagId(request.getTagId())
            .build();

    return MessageTemplateResponse.from(messageTemplateRepository.save(template));
  }

  @Override
  @Transactional
  public void changeTemplate(Long templateId, Long shopId, MessageTemplateUpdateRequest request) {
    MessageTemplate template =
        messageTemplateQueryRepository
            .findByIdAndNotDeleted(templateId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
    System.out.println(shopId);
    if (!template.isUsableForShop(shopId)) {
      throw new BusinessException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }

    template.update(
        request.getTemplateName(),
        request.getTemplateContent(),
        request.getTemplateType(),
        request.getCustomerGradeId(),
        request.getTagId());
  }

  @Transactional
  @Override
  public void removeTemplate(Long templateId, Long shopId) {
    MessageTemplate template =
        messageTemplateQueryRepository
            .findByIdAndNotDeleted(templateId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

    if (!template.isUsableForShop(shopId)) {
      throw new BusinessException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }

    template.softDelete();
  }
}
