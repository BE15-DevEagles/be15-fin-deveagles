package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageSettingRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSettingResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageSettingsCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "메시지 설정", description = "메시지 설정 관련 API")
@RestController
@RequestMapping("/message/settings")
@RequiredArgsConstructor
public class MessageSettingsController {
  private final MessageSettingsCommandService messageSettingsCommandService;

  @Operation(summary = "문자 설정 기본값 생성", description = "해당 매장(shopId)의 문자 설정을 기본값으로 생성합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "설정 생성 완료"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "유효하지 않은 shopId")
  })
  @PostMapping()
  public ResponseEntity<ApiResponse<Long>> create(@AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    Long id = messageSettingsCommandService.createDefault(shopId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shopId));
  }

  @Operation(summary = "문자 설정 조회", description = "현재 로그인한 매장의 문자 설정 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "설정 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "설정 정보 없음")
  })
  @GetMapping()
  public ResponseEntity<ApiResponse<MessageSettingResponse>> getSettings(
      @AuthenticationPrincipal CustomUser customUser) {
    Long shopId = customUser.getShopId();
    return ResponseEntity.ok(
        ApiResponse.success(messageSettingsCommandService.loadSettings(shopId)));
  }

  @Operation(summary = "문자 설정 수정", description = "현재 로그인한 매장의 문자 설정을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "설정 수정 완료"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 데이터 오류")
  })
  @PutMapping()
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUser customUser,
      @Valid @RequestBody MessageSettingRequest request) {
    Long shopId = customUser.getShopId();
    messageSettingsCommandService.updateSettings(shopId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
