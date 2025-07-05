package com.deveagles.be15_deveagles_be.features.messages.query.service;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import org.springframework.data.domain.Pageable;

public interface MessageTemplateQueryService {
  PagedResult<MessageTemplateResponse> findAll(Long shopId, Pageable pageable);

  MessageTemplateResponse findOne(Long shopId, Long templateId);
}
