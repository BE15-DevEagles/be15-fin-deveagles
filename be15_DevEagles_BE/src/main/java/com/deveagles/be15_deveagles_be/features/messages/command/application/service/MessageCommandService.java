package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.UpdateReservationRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSendResult;
import java.util.Collection;
import java.util.List;

public interface MessageCommandService {
  List<MessageSendResult> sendSms(Long shopId, SmsRequest smsRequest);

  void markSmsAsFailed(Collection<Long> smsIds);

  void markSmsAsSent(Collection<Long> smsIds);

  void updateReservationMessage(
      UpdateReservationRequest updateReservationRequest, Long shopId, Long messageId);

  MessageSendResult resendFailedMessage(Long shopId, Long messageId);

  void cancelScheduledMessage(Long messageId, Long shopId);
}
