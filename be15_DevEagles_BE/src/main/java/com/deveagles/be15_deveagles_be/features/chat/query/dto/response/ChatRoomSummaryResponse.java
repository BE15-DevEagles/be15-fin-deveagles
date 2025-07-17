package com.deveagles.be15_deveagles_be.features.chat.query.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryResponse {
  private String roomId;
  private String customerName; // 상담사용
  private String customerShopName; // 상담사용
  private String lastMessage;
  private LocalDateTime lastMessageAt;
}
