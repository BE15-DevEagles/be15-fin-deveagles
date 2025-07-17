package com.deveagles.be15_deveagles_be.features.chat.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.chat.query.dto.response.ChatRoomSummaryResponse;
import com.deveagles.be15_deveagles_be.features.chat.query.service.ChatRoomQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅방 조회", description = "채팅방 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chatrooms")
public class ChatRoomQueryController {

  private final ChatRoomQueryService chatRoomQueryService;

  @Operation(
      summary = "내 채팅방 목록 조회",
      description = "현재 로그인한 사용자의 채팅방 목록을 조회합니다. 상담사일 경우 자신에게 배정된 채팅방만 조회됩니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채팅방 목록 조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "권한 없음 또는 로그인 정보 누락")
  })
  @GetMapping()
  public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getMyChatRooms(
      @AuthenticationPrincipal CustomUser customUser) {
    boolean isStaff = (customUser.getUserId() == 17); // 또는 Role 기반으로 변경 가능
    List<ChatRoomSummaryResponse> result =
        chatRoomQueryService.getMyChatRooms(
            customUser.getStaffName(), customUser.getUserId(), isStaff);
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
