package com.deveagles.be15_deveagles_be.features.messages.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.service.MessageTemplateQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "템플릿 조회", description = "템플릿 조회 관련 API")
@RestController
@RequestMapping("/message/templates")
@RequiredArgsConstructor
public class MessageTemplateQueryController {
  private final MessageTemplateQueryService messageTemplateQueryService;

  @Operation(summary = "템플릿 목록 조회", description = "현재 로그인한 매장의 전체 메시지 템플릿 목록을 페이지네이션과 함께 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공")
  })
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

  @Operation(summary = "단일 템플릿 조회", description = "templateId를 통해 특정 템플릿 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "템플릿을 찾을 수 없음")
  })
  @GetMapping("/{templateId}")
  public ResponseEntity<ApiResponse<MessageTemplateResponse>> getTemplate(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long templateId) {
    MessageTemplateResponse response =
        messageTemplateQueryService.findOne(customUser.getShopId(), templateId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
