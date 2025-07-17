package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.UpdateReservationRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSendResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "메시지 발송", description = "메시지 발송 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/message")
public class MessageCommandController {

  private final MessageCommandService messageCommandService;

  @Operation(summary = "문자 메시지 발송", description = "매장의 선택된 고객에게 문자 메시지를 발송합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "발송 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청")
  })
  @PostMapping("/send")
  public ResponseEntity<ApiResponse<List<MessageSendResult>>> sendSms(
      @AuthenticationPrincipal CustomUser customUser, @RequestBody @Valid SmsRequest request) {

    List<MessageSendResult> resultList =
        messageCommandService.sendSms(customUser.getShopId(), request);
    return ResponseEntity.ok(ApiResponse.success(resultList));
  }

  @Operation(summary = "예약 문자 수정", description = "이미 등록된 예약 문자의 전송 내용을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "수정 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "해당 메시지를 찾을 수 없음")
  })
  @PutMapping("/{messageId}")
  public ResponseEntity<ApiResponse<Void>> updateReservation(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid UpdateReservationRequest updateReservationRequest,
      @PathVariable Long messageId) {
    messageCommandService.updateReservationMessage(
        updateReservationRequest, customUser.getShopId(), messageId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @Operation(summary = "예약 문자 취소", description = "등록된 예약 문자 전송을 취소합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "취소 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "해당 메시지를 찾을 수 없음")
  })
  @DeleteMapping("/{messageId}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancelReservation(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long messageId) {
    messageCommandService.cancelScheduledMessage(messageId, customUser.getShopId());

    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/{messageId}/resend")
  public ResponseEntity<ApiResponse<MessageSendResult>> resendFailedMessage(
      @AuthenticationPrincipal CustomUser user, @PathVariable Long messageId) {

    Long shopId = user.getShopId();
    MessageSendResult result = messageCommandService.resendFailedMessage(shopId, messageId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
