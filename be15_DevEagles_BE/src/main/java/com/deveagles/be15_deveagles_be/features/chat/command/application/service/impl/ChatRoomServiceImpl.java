package com.deveagles.be15_deveagles_be.features.chat.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatRoomCreateResult;
import com.deveagles.be15_deveagles_be.features.chat.command.application.service.ChatRoomService;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomServiceImpl implements ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ShopCommandService shopCommandService;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public ChatRoomCreateResult createChatRoom(Long shopId, Long staffId, String staffName) {
    shopCommandService.validateShopExists(shopId);
    String shopName = shopCommandService.getShop(shopId).getShopName();

    ChatRoom.Participant participant =
        ChatRoom.Participant.builder()
            .id(staffId)
            .name(staffName)
            .shopId(shopId)
            .shopName(shopName)
            .build();

    ChatRoom chatRoom =
        ChatRoom.builder()
            .participant(participant)
            .isAiActive(true)
            .createdAt(LocalDateTime.now())
            .build();

    ChatRoom saved = chatRoomRepository.save(chatRoom);

    // ❌ 메시지 저장 제거!
    return new ChatRoomCreateResult(saved.getId());
  }

  @Override
  public void sendGreeting(String roomId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

    ChatMessage message =
        chatMessageRepository.save(
            ChatMessage.builder()
                .chatroomId(roomId)
                .sender(ChatMessage.Sender.builder().id(null).name("Beautifly AI").build())
                .content("어떤 도움이 필요하신가요?")
                .isCustomer(false)
                .createdAt(LocalDateTime.now())
                .build());

    ChatMessageResponse response =
        ChatMessageResponse.builder()
            .messageId(message.getId())
            .chatroomId(roomId)
            .senderId(null)
            .senderName("Beautifly AI")
            .content(message.getContent())
            .isCustomer(false)
            .createdAt(message.getCreatedAt())
            .build();

    messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, response);
  }

  @Override
  public void switchToStaff(String roomId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

    if (!chatRoom.isAiActive()) {
      log.info("[SWITCH] 이미 상담사로 전환된 채팅방: {}", roomId);
      return;
    }

    Long staffId = 17L; // 지금은 하드코딩 (임시 상담사 ID)

    chatRoom.updateToStaff(staffId); // isAiActive = false, assignedStaffId = 17L
    chatRoomRepository.save(chatRoom);

    ChatMessage message =
        ChatMessage.builder()
            .chatroomId(roomId)
            .sender(ChatMessage.Sender.builder().id(staffId).name("상담사").build())
            .content("상담사가 연결되었습니다. 무엇을 도와드릴까요?")
            .isCustomer(false)
            .createdAt(LocalDateTime.now())
            .build();

    chatMessageRepository.save(message);

    messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, message);
  }
}
