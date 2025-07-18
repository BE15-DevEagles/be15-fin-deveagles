package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.AutomaticCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.AutomaticTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "자동 발송 관리", description = "자동 발송 관련 설정, 수정, 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/message/auto-message")
public class AutomaticMessageTriggerController {
  private final AutomaticMessageTriggerService automaticMessageTriggerService;

  @Operation(summary = "자동발송 메시지 등록", description = "특정 매장의 자동발송 메시지를 등록합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "등록 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> registerAutoMessage(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid AutomaticCreateRequest automaticCreateRequest) {
    automaticMessageTriggerService.registerAutomaticMessage(
        customUser.getShopId(), automaticCreateRequest);

    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<AutomaticTemplateResponse>>> getTemplates(
      @AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId(); //
    List<AutomaticTemplateResponse> templates =
        automaticMessageTriggerService.getAutomaticMessages(shopId);
    return ResponseEntity.ok(ApiResponse.success(templates));
  }
}
