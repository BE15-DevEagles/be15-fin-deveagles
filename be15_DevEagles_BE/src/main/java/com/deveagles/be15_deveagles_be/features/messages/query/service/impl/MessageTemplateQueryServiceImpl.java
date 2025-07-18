package com.deveagles.be15_deveagles_be.features.messages.query.service.impl;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.service.MessageTemplateQueryService;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageTemplateQueryServiceImpl implements MessageTemplateQueryService {
  private final ShopCommandService shopCommandService;
  private final MessageTemplateQueryRepository messageTemplateQueryRepository;

  @Override
  public PagedResult<MessageTemplateResponse> findAll(Long shopId, Pageable pageable) {
    shopCommandService.validateShopExists(shopId);
    Page<MessageTemplate> page = messageTemplateQueryRepository.findAllByShopId(shopId, pageable);

    Page<MessageTemplateResponse> mapped = page.map(MessageTemplateResponse::from);

    return PagedResult.from(mapped); //  변경 포인트
  }

  @Override
  public MessageTemplateResponse findOne(Long shopId, Long templateId) {
    shopCommandService.validateShopExists(shopId);
    return messageTemplateQueryRepository
        .findByIdAndNotDeleted(templateId)
        .map(MessageTemplateResponse::from)
        .orElseThrow(() -> new RuntimeException("템플릿이 존재하지 않습니다."));
  }
}
