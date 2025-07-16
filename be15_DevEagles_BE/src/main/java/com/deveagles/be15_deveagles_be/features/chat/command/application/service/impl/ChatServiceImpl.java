package com.deveagles.be15_deveagles_be.features.chat.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.request.ChatMessageRequest;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.service.AiMessageService;
import com.deveagles.be15_deveagles_be.features.chat.command.application.service.ChatService;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final AiMessageService aiMessageService;

  @Override
  public ChatMessageResponse saveMessage(ChatMessageRequest request, Long userId) {
    // 채팅방 검증
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(request.getRoomId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

    // 메시지 저장
    ChatMessage saved =
        chatMessageRepository.save(
            ChatMessage.builder()
                .chatroomId(chatRoom.getId())
                .sender(
                    ChatMessage.Sender.builder().id(userId).name(request.getSenderName()).build())
                .content(request.getContent())
                .isCustomer(request.isCustomer())
                .createdAt(LocalDateTime.now())
                .build());

    // 응답 DTO 생성
    ChatMessageResponse response =
        ChatMessageResponse.builder()
            .messageId(saved.getId())
            .chatroomId(saved.getChatroomId())
            .senderId(saved.getSender().getId())
            .senderName(saved.getSender().getName())
            .content(saved.getContent())
            .isCustomer(saved.isCustomer())
            .createdAt(saved.getCreatedAt())
            .build();

    // WebSocket 브로커 전송
    messagingTemplate.convertAndSend("/sub/chatroom/" + chatRoom.getId(), response);
    if (chatRoom.isAiActive()) {
      aiMessageService.handleAiResponse(chatRoom.getId(), request.getContent());
    }
    return response;
  }
}
