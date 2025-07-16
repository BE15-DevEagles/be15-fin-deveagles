package com.deveagles.be15_deveagles_be.features.chat.command.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AiMessageServiceTest {

  @InjectMocks private AiMessageService aiMessageService;

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private ChatMessageRepository chatMessageRepository;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Mock private WebClient webClient;

  @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;

  @Mock private WebClient.RequestBodySpec requestBodySpec;

  @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;

  @Mock private WebClient.ResponseSpec responseSpec;

  @BeforeEach
  void setUp() {
    aiMessageService =
        new AiMessageService(
            chatRoomRepository, chatMessageRepository, messagingTemplate, webClient);
    ReflectionTestUtils.setField(aiMessageService, "aiWebhookUrl", "http://mocked.url");
  }

  @Test
  void handleAiResponse_success() {
    // given
    String roomId = "room123";
    String input = "안녕하세요";

    ChatRoom chatRoom =
        ChatRoom.builder()
            .id(roomId)
            .isAiActive(true)
            .participant(ChatRoom.Participant.builder().id(1L).name("고객").build())
            .createdAt(LocalDateTime.now())
            .build();

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));

    // mocking WebClient fluent chain
    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(JsonNode.class))
        .thenReturn(
            Mono.just(new ObjectMapper().createObjectNode().put("output", "안녕하세요! 무엇을 도와드릴까요?")));

    when(chatMessageRepository.save(any(ChatMessage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    aiMessageService.handleAiResponse(roomId, input);

    // then
    verify(chatRoomRepository, times(1)).findById(roomId);
    verify(webClient, times(1)).post();
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    verify(messagingTemplate, times(1))
        .convertAndSend(eq("/sub/chatroom/" + roomId), any(ChatMessageResponse.class));
  }
}
