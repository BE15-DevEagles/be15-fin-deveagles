package com.deveagles.be15_deveagles_be.features.chat.command.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatMessageResponse;
import com.deveagles.be15_deveagles_be.features.chat.command.application.dto.response.ChatRoomCreateResult;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatMessage;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate.ChatRoom;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatMessageRepository;
import com.deveagles.be15_deveagles_be.features.chat.command.domain.repository.ChatRoomRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.dto.response.GetShopResponse;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

  @InjectMocks private ChatRoomServiceImpl chatRoomService;

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private ChatMessageRepository chatMessageRepository;

  @Mock private ShopCommandService shopCommandService;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Test
  void createChatRoom_성공() {
    // given
    Long shopId = 1L;
    Long staffId = 10L;
    String staffName = "홍길동";
    String shopName = "테스트매장";

    GetShopResponse response = GetShopResponse.builder().shopName(shopName).build();

    when(shopCommandService.getShop(shopId)).thenReturn(response);

    ChatRoom savedRoom =
        ChatRoom.builder()
            .id("room-123")
            .participant(
                ChatRoom.Participant.builder()
                    .id(staffId)
                    .name(staffName)
                    .shopId(shopId)
                    .shopName(shopName)
                    .build())
            .isAiActive(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(chatRoomRepository.save(any())).thenReturn(savedRoom);

    // when
    ChatRoomCreateResult result = chatRoomService.createChatRoom(shopId, staffId, staffName);

    // then
    verify(shopCommandService).validateShopExists(shopId);
    verify(shopCommandService).getShop(shopId);
    verify(chatRoomRepository).save(any());

    assertNotNull(result);
    assertEquals("room-123", result.roomId()); //
  }

  @Test
  void sendGreeting_성공() {
    // given
    String roomId = "abc123";
    ChatRoom chatRoom = ChatRoom.builder().id(roomId).isAiActive(true).build();

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));

    ChatMessage savedMessage =
        ChatMessage.builder()
            .id("msg001")
            .chatroomId(roomId)
            .content("어떤 도움이 필요하신가요?")
            .isCustomer(false)
            .createdAt(LocalDateTime.now())
            .build();

    when(chatMessageRepository.save(any())).thenReturn(savedMessage);

    // when
    chatRoomService.sendGreeting(roomId);

    // then
    verify(chatRoomRepository).findById(roomId);
    verify(chatMessageRepository).save(any());
    verify(messagingTemplate)
        .convertAndSend(eq("/sub/chatroom/" + roomId), any(ChatMessageResponse.class));
  }

  @Test
  void switchToStaff_성공() {
    // given
    String roomId = "room123";
    ChatRoom chatRoom = ChatRoom.builder().id(roomId).isAiActive(true).build();

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
    when(chatRoomRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    chatRoomService.switchToStaff(roomId);

    // then
    verify(chatRoomRepository).findById(roomId);
    verify(chatRoomRepository).save(any());
    verify(chatMessageRepository).save(any());
    verify(messagingTemplate).convertAndSend(eq("/sub/chatroom/" + roomId), any(ChatMessage.class));

    assertFalse(chatRoom.isAiActive());
    assertEquals(17L, chatRoom.getAssignedStaffId());
  }
}
