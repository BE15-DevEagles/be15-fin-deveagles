package com.deveagles.be15_deveagles_be.features.notifications.command.application.dto;

import com.deveagles.be15_deveagles_be.features.notifications.command.domain.aggregate.NotificationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateNotificationRequest {
  private final Long shopId;
  private final NotificationType type;
  private final String title;
  private final String content;
}
