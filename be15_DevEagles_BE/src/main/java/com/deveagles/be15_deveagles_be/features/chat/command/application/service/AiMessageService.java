package com.deveagles.be15_deveagles_be.features.chat.command.application.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import com.deveagles.be15_deveagles_be.features.chat.config.WebClientConfig.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final WebClient webClient;

  @Value("${webhook.ai-url}") // .env에서 관리
  private String aiWebhookUrl;

  public void handleAiResponse(String roomId, String userInput) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

    String aiOutput;
    try {
      aiOutput =
          webClient
              .post()
              .uri(aiWebhookUrl)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(
                  Map.of(
                      "sessionId", roomId,
                      "chatInput", userInput,
                      "action", "sendMessage"))
              .retrieve()
              .bodyToMono(JsonNode.class)
              .map(json -> json.get("output").asText())
              .block();
    } catch (Exception e) {
      log.error("[AI] Webhook 호출 실패", e);
      return;
    }

    ChatMessage message =
        ChatMessage.builder()
            .chatroomId(roomId)
            .sender(ChatMessage.Sender.builder().id(null).name("Beautifly AI").build())
            .content(aiOutput)
            .isCustomer(false)
            .createdAt(LocalDateTime.now())
            .build();

    chatMessageRepository.save(message);

    ChatMessageResponse response = ChatMessageResponse.from(message);
    messagingTemplate.convertAndSend("/sub/chatroom/" + roomId, response);
  }
}
