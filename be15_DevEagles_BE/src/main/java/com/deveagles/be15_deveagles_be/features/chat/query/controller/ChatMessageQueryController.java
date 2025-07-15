package com.deveagles.be15_deveagles_be.features.chat.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.query.service.ChatMessageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅 메시지 조회", description = "채팅 메시지 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chatrooms")
public class ChatMessageQueryController {

  private final ChatMessageQueryService chatMessageQueryService;

  @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 모든 메시지를 시간순으로 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음")
  })
  @GetMapping("/{roomId}/messages")
  public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
      @PathVariable String roomId) {
    List<ChatMessageResponse> messages = chatMessageQueryService.getMessagesByRoomId(roomId);
    return ResponseEntity.ok(ApiResponse.success(messages));
  }
}
