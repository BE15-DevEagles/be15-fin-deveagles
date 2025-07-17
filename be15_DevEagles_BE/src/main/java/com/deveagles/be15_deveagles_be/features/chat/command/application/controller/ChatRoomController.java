package com.deveagles.be15_deveagles_be.features.chat.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatRoomCreateResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatRoomCreateResult;
import com.deveagles.be15_deveagles_be.features.chat.command.application.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "채팅방 관리", description = "채팅방 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chatrooms")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @Operation(summary = "채팅방 생성", description = "현재 로그인한 매장 사용자의 채팅방을 생성합니다. 같은 유저는 중복 생성되지 않습니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채팅방 생성 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "중복 요청 또는 매장 정보 오류")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> createRoom(
      @AuthenticationPrincipal CustomUser customUser) {

    Long shopId = customUser.getShopId();
    Long staffId = customUser.getUserId();
    String staffName = customUser.getStaffName();

    ChatRoomCreateResult result = chatRoomService.createChatRoom(shopId, staffId, staffName);

    return ResponseEntity.ok(ApiResponse.success(new ChatRoomCreateResponse(result.roomId())));
  }

  @Operation(
      summary = "초기 인사 메시지 전송",
      description = "WebSocket 구독 이후, AI 챗봇의 인사 메시지를 해당 채팅방에 전송합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "인사 메시지 전송 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음")
  })
  @PostMapping("/{roomId}/send-greeting")
  public ResponseEntity<Void> sendGreeting(@PathVariable String roomId) {
    chatRoomService.sendGreeting(roomId); // 👈 Impl 호출
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "상담사 전환 처리", description = "AI 챗봇 응답을 중단하고, 상담사 응답 모드로 전환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "상담사 전환 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음")
  })
  @PostMapping("/{roomId}/switch-to-staff")
  public ResponseEntity<ApiResponse<Void>> switchToStaff(@PathVariable String roomId) {
    chatRoomService.switchToStaff(roomId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
