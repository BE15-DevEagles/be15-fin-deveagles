package com.deveagles.be15_deveagles_be.features.chat.query.service.impl;

import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.query.dto.response.ChatRoomSummaryResponse;
import com.deveagles.be15_deveagles_be.features.chat.query.repository.ChatRoomQueryRepository;
import com.deveagles.be15_deveagles_be.features.chat.query.service.ChatRoomQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryServiceImpl implements ChatRoomQueryService {

  private final ChatRoomQueryRepository chatRoomQueryRepository;
  private final ChatMessageRepository chatMessageRepository;

  @Override
  public List<ChatRoomSummaryResponse> getMyChatRooms(
      String userName, Long userId, boolean isStaff) {
    List<ChatRoom> rooms =
        isStaff
            ? chatRoomQueryRepository.findByAssignedStaffIdAndIsAiActiveFalse(userId)
            : chatRoomQueryRepository.findByParticipantId(userId);

    return rooms.stream().map(room -> toSummaryResponse(room, isStaff)).toList();
  }

  private ChatRoomSummaryResponse toSummaryResponse(ChatRoom room, boolean isStaff) {
    Optional<ChatMessage> latestMessageOpt =
        chatMessageRepository.findTopByChatroomIdOrderByCreatedAtDesc(room.getId());

    LocalDateTime lastMessageAt = latestMessageOpt.map(ChatMessage::getCreatedAt).orElse(null);
    String lastMessage = latestMessageOpt.map(ChatMessage::getContent).orElse(null);

    ChatRoomSummaryResponse.ChatRoomSummaryResponseBuilder builder =
        ChatRoomSummaryResponse.builder()
            .roomId(room.getId())
            .lastMessage(lastMessage)
            .lastMessageAt(lastMessageAt);

    if (isStaff) {
      // 상담사 → 고객 정보 보여줌
      ChatRoom.Participant p = room.getParticipant();
      String customerName = (p != null && p.getName() != null) ? p.getName() : "-";
      String customerShopName = (p != null && p.getShopName() != null) ? p.getShopName() : "-";

      builder.customerName(customerName).customerShopName(customerShopName);
    } else {
      // 고객 → Beautifly 정보 고정으로 보여줌
      builder.customerName("Beautifly").customerShopName("Beautifly 본사");
    }

    return builder.build();
  }
}
