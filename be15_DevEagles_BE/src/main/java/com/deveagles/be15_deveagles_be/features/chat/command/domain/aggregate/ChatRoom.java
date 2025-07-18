package com.deveagles.be15_deveagles_be.features.chat.command.domain.aggregate;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chatroom")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
  @Id private String id;

  private Participant participant;
  private boolean isAiActive;
  private LocalDateTime createdAt;
  private LocalDateTime deletedAt;
  private Long assignedStaffId; // 추가: 상담사 ID

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Participant {
    private Long id;
    private String name;
    private Long shopId;
    private String shopName;
  }

  public void updateToStaff(Long staffId) {
    this.isAiActive = false;
    this.assignedStaffId = staffId;
  }
}
