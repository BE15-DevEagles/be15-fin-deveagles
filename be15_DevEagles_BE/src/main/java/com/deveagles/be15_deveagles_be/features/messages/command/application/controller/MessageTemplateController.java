package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateUpdateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "템플릿 관리", description = "템플릿 관련 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/message/templates")
public class MessageTemplateController {
  private final MessageTemplateService messageTemplateService;

  @Operation(summary = "메시지 템플릿 생성", description = "현재 로그인한 매장에 새로운 메시지 템플릿을 생성합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "템플릿 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 입력값"),
  })
  @PostMapping()
  public ResponseEntity<ApiResponse<MessageTemplateResponse>> create(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid MessageTemplateCreateRequest templateCreateRequest) {
    MessageTemplateResponse response =
        messageTemplateService.createTemplate(customUser.getShopId(), templateCreateRequest);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "메시지 템플릿 수정", description = "템플릿 ID를 기준으로 기존 템플릿을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "템플릿 수정 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "템플릿을 찾을 수 없음"),
  })
  @PutMapping("/{templateId}")
  public ResponseEntity<ApiResponse<Void>> updateTemplate(
      @AuthenticationPrincipal CustomUser customUser,
      @PathVariable Long templateId,
      @Valid @RequestBody MessageTemplateUpdateRequest messageTemplateUpdateRequest) {
    messageTemplateService.changeTemplate(
        templateId, customUser.getShopId(), messageTemplateUpdateRequest);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "메시지 템플릿 삭제", description = "템플릿 ID를 기준으로 템플릿을 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "템플릿 삭제 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "템플릿을 찾을 수 없음"),
  })
  @DeleteMapping("/{templateId}")
  public ResponseEntity<ApiResponse<Void>> deleteTemplate(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long templateId) {
    messageTemplateService.removeTemplate(templateId, customUser.getShopId());

    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
