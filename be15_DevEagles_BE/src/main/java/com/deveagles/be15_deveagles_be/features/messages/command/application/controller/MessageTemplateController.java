package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateUpdateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/message/templates")
public class MessageTemplateController {
  private final MessageTemplateService messageTemplateService;

  @PostMapping()
  public ResponseEntity<ApiResponse<MessageTemplateResponse>> create(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid MessageTemplateCreateRequest templateCreateRequest) {
    MessageTemplateResponse response =
        messageTemplateService.createTemplate(customUser.getShopId(), templateCreateRequest);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping("/{templateId}")
  public ResponseEntity<ApiResponse<Void>> updateTemplate(
      @AuthenticationPrincipal CustomUser customUser,
      @PathVariable Long templateId,
      @Valid @RequestBody MessageTemplateUpdateRequest messageTemplateUpdateRequest) {
    messageTemplateService.changeTemplate(
        templateId, customUser.getShopId(), messageTemplateUpdateRequest);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/{templateId}")
  public ResponseEntity<ApiResponse<Void>> deleteTemplate(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long templateId) {
    messageTemplateService.removeTemplate(templateId, customUser.getShopId());

    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
