package com.deveagles.be15_deveagles_be.features.messages.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.service.MessageTemplateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message/templates")
@RequiredArgsConstructor
public class MessageTemplateQueryController {
  private final MessageTemplateQueryService messageTemplateQueryService;

  @GetMapping
  public ResponseEntity<ApiResponse<PagedResult<MessageTemplateResponse>>> getTemplates(
      @AuthenticationPrincipal CustomUser customUser,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Long shopId = customUser.getShopId();
    PagedResult<MessageTemplateResponse> result =
        messageTemplateQueryService.findAll(shopId, pageable);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping("/{templateId}")
  public ResponseEntity<ApiResponse<MessageTemplateResponse>> getTemplate(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long templateId) {
    MessageTemplateResponse response =
        messageTemplateQueryService.findOne(customUser.getShopId(), templateId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
