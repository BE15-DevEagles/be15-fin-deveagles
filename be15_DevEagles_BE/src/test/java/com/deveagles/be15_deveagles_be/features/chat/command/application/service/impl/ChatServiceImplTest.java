package com.deveagles.be15_deveagles_be.features.chat.command.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.request.ChatMessageRequest;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.service.AiMessageService;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("채팅 메시지 저장 기능 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

  @InjectMocks private ChatServiceImpl chatService;

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private ChatMessageRepository chatMessageRepository;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Mock private AiMessageService aiMessageService;

  @Test
  @DisplayName("AI 활성화된 채팅방에서 메시지 전송 시 Webhook 호출이 수행된다")
  void saveMessage_success_withAiCall() {
    // given
    String roomId = "room-123";
    Long userId = 10L;

    ChatRoom chatRoom =
        ChatRoom.builder()
            .id(roomId)
            .isAiActive(true)
            .participant(ChatRoom.Participant.builder().id(1L).name("고객").build())
            .createdAt(LocalDateTime.now())
            .build();

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .roomId(roomId)
            .senderName("고객")
            .content("안녕하세요")
            .isCustomer(true)
            .build();

    ChatMessage mockSavedMessage =
        ChatMessage.builder()
            .id("msg-1")
            .chatroomId(roomId)
            .sender(ChatMessage.Sender.builder().id(userId).name("고객").build())
            .content("안녕하세요")
            .isCustomer(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockSavedMessage);

    // when
    ChatMessageResponse response = chatService.saveMessage(request, userId);

    // then
    assertThat(response.getChatroomId()).isEqualTo(roomId);
    assertThat(response.getContent()).isEqualTo("안녕하세요");
    assertThat(response.getSenderName()).isEqualTo("고객");

    verify(chatRoomRepository, times(1)).findById(roomId);
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    verify(messagingTemplate, times(1))
        .convertAndSend(eq("/sub/chatroom/" + roomId), any(Object.class));
    verify(aiMessageService, times(1)).handleAiResponse(roomId, "안녕하세요");
  }

  @Test
  @DisplayName("AI 비활성화된 채팅방에서는 Webhook 호출이 수행되지 않는다")
  void saveMessage_aiInactive_shouldNotCallAi() {
    // given
    String roomId = "room-456";
    Long userId = 20L;

    ChatRoom chatRoom =
        ChatRoom.builder()
            .id(roomId)
            .isAiActive(false)
            .participant(ChatRoom.Participant.builder().id(2L).name("고객2").build())
            .createdAt(LocalDateTime.now())
            .build();

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .roomId(roomId)
            .senderName("고객2")
            .content("도와주세요")
            .isCustomer(true)
            .build();

    ChatMessage savedMessage =
        ChatMessage.builder()
            .id("dummy-message-id")
            .chatroomId(roomId)
            .sender(ChatMessage.Sender.builder().id(userId).name("고객2").build())
            .content("도와주세요")
            .isCustomer(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

    // when
    chatService.saveMessage(request, userId);

    // then
    verify(aiMessageService, never()).handleAiResponse(any(), any());
  }
}
