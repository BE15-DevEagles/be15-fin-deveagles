package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.UpdateReservationRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSendResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageClickService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageVariableProcessor;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.*;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageSettingRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.SmsRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.infrastructure.CoolSmsClient;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageCommandServiceImplTest {

  @InjectMocks private MessageCommandServiceImpl messageCommandService;
  @Mock private ShopCommandService shopCommandService;
  @Mock private CustomerQueryService customerQueryService;
  @Mock private MessageSettingRepository messageSettingRepository;
  @Mock private CoolSmsClient coolSmsClient;
  @Mock private SmsRepository smsRepository;
  @Mock private MessageVariableProcessor messageVariableProcessor;
  @Mock private MessageClickService messageClickService;
  private final Long shopId = 1L;

  private SmsRequest immediateRequest() {
    return new SmsRequest(
        List.of(1L),
        "안녕하세요 #{고객명}",
        MessageType.SMS,
        MessageSendingType.IMMEDIATE,
        null,
        10L,
        true,
        11L,
        12L,
        MessageKind.announcement,
        13L,
        14L,
        null);
  }

  private SmsRequest reservationRequest(LocalDateTime when) {
    return new SmsRequest(
        List.of(1L),
        "예약 메시지 #{고객명}",
        MessageType.SMS,
        MessageSendingType.RESERVATION,
        when,
        10L,
        true,
        11L,
        12L,
        MessageKind.announcement,
        13L,
        14L,
        null);
  }

  @Test
  @DisplayName("즉시 발송 성공")
  void sendSms_immediate_success() {
    // given
    SmsRequest request = immediateRequest();
    LocalDateTime now = LocalDateTime.now();
    List<Long> customerIds = List.of(1L);
    List<String> phones = List.of("01011112222");

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("07000000000")
            .canAlimtalk(true)
            .point(1000L)
            .build();

    when(customerQueryService.getCustomerPhoneNumbers(customerIds)).thenReturn(phones);
    when(messageSettingRepository.findByShopId(shopId)).thenReturn(Optional.of(settings));

    // ✅ 인자 정확히 안 맞아도 되도록 any() 사용
    when(messageVariableProcessor.buildPayload(anyLong(), anyLong(), any()))
        .thenReturn(Map.of("고객명", "신사임당"));

    when(messageVariableProcessor.resolveVariables(any(), any())).thenReturn("안녕하세요 홍길동");

    Sms sms = Sms.builder().messageId(1L).messageContent("안녕하세요 홍길동").build();
    when(smsRepository.saveAll(anyList())).thenReturn(List.of(sms));

    when(coolSmsClient.sendMany(any(), any(), anyList()))
        .thenReturn(List.of(new MessageSendResult(true, "성공", 1L)));

    // when
    List<MessageSendResult> result = messageCommandService.sendSms(shopId, request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).success()).isTrue();
  }

  @Test
  @DisplayName("예약 발송 성공")
  void sendSms_reservation_success() {
    SmsRequest request = reservationRequest(LocalDateTime.now().plusMinutes(10));

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("07000000000")
            .point(1000L)
            .canAlimtalk(true)
            .build();

    when(customerQueryService.getCustomerPhoneNumbers(any())).thenReturn(List.of("01011112222"));
    when(messageSettingRepository.findByShopId(shopId)).thenReturn(Optional.of(settings));
    when(messageVariableProcessor.buildPayload(anyLong(), anyLong(), isNull()))
        .thenReturn(Map.of("고객명", "신사임당"));
    when(messageVariableProcessor.resolveVariables(any(), any())).thenReturn("예약 메시지 신사임당");

    Sms sms = Sms.builder().messageId(1L).messageContent("예약 메시지 신사임당").build();
    when(smsRepository.saveAll(anyList())).thenReturn(List.of(sms));

    List<MessageSendResult> result = messageCommandService.sendSms(shopId, request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).resultMessage()).isEqualTo("예약 등록 완료");
    verifyNoInteractions(coolSmsClient);
  }

  @Test
  @DisplayName("즉시 발송 성공 - 링크 포함 시 치환 처리")
  void sendSms_immediate_withLink_success() {
    SmsRequest request = immediateRequest();
    List<String> phones = List.of("01012345678");

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("07000000000")
            .canAlimtalk(true)
            .point(1000L)
            .build();

    when(customerQueryService.getCustomerPhoneNumbers(any())).thenReturn(phones);
    when(messageSettingRepository.findByShopId(shopId)).thenReturn(Optional.of(settings));
    when(messageVariableProcessor.buildPayload(anyLong(), anyLong(), isNull()))
        .thenReturn(Map.of("고객명", "홍길동"));
    when(messageVariableProcessor.resolveVariables(any(), any())).thenReturn("안녕하세요 #{프로필링크}");

    Sms sms = Sms.builder().messageId(1L).messageContent("안녕하세요 #{프로필링크}").hasLink(true).build();

    when(smsRepository.saveAll(anyList())).thenReturn(List.of(sms));
    when(messageClickService.createTrackableLink(eq(1L), any()))
        .thenReturn("http://track.com/token123");
    when(coolSmsClient.sendMany(any(), any(), anyList()))
        .thenReturn(List.of(new MessageSendResult(true, "성공", 1L)));

    List<MessageSendResult> result = messageCommandService.sendSms(shopId, request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).success()).isTrue();
    verify(messageClickService).createTrackableLink(eq(1L), contains(shopId.toString()));
  }

  @Test
  @DisplayName("예약 발송 성공 - 링크 포함 시 치환 처리")
  void sendSms_reservation_withLink_success() {
    SmsRequest request = reservationRequest(LocalDateTime.now().plusMinutes(10));
    List<String> phones = List.of("01012345678");

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("07000000000")
            .canAlimtalk(true)
            .point(1000L)
            .build();

    when(customerQueryService.getCustomerPhoneNumbers(any())).thenReturn(phones);
    when(messageSettingRepository.findByShopId(shopId)).thenReturn(Optional.of(settings));
    when(messageVariableProcessor.buildPayload(anyLong(), anyLong(), isNull()))
        .thenReturn(Map.of("고객명", "홍길동"));
    when(messageVariableProcessor.resolveVariables(any(), any())).thenReturn("예약 메시지 #{프로필링크}");

    Sms sms = Sms.builder().messageId(1L).messageContent("예약 메시지 #{프로필링크}").hasLink(true).build();

    when(smsRepository.saveAll(anyList())).thenReturn(List.of(sms));
    when(messageClickService.createTrackableLink(eq(1L), any()))
        .thenReturn("http://track.com/token456");

    List<MessageSendResult> result = messageCommandService.sendSms(shopId, request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).resultMessage()).isEqualTo("예약 등록 완료");
    verify(messageClickService).createTrackableLink(eq(1L), contains(shopId.toString()));
    verifyNoInteractions(coolSmsClient);
  }

  @Test
  @DisplayName("예약 메시지 수정 성공")
  void updateReservationMessage_success() {
    Sms sms = mock(Sms.class);
    UpdateReservationRequest req =
        new UpdateReservationRequest(
            "수정된 메시지", MessageKind.announcement, 999L, LocalDateTime.now().plusMinutes(30));

    when(sms.getShopId()).thenReturn(shopId);
    when(sms.getScheduledAt()).thenReturn(req.scheduledAt());
    when(smsRepository.findById(1L)).thenReturn(Optional.of(sms));

    messageCommandService.updateReservationMessage(req, shopId, 1L);
    verify(sms)
        .updateReservation(
            req.messageContent(), req.messageKind(), req.customerId(), req.scheduledAt());
  }

  @Test
  @DisplayName("예약 메시지 취소 성공")
  void cancelScheduledMessage_success() {
    Sms sms = mock(Sms.class);
    when(sms.getShopId()).thenReturn(shopId);
    when(sms.isReservable()).thenReturn(true);
    when(smsRepository.findById(1L)).thenReturn(Optional.of(sms));

    messageCommandService.cancelScheduledMessage(1L, shopId);
    verify(sms).cancel();
  }

  @Test
  @DisplayName("재발송 성공")
  void resendFailedMessage_success() {
    Sms sms =
        Sms.builder()
            .messageId(1L)
            .shopId(shopId)
            .customerId(1L)
            .messageContent("실패된 메시지 #{고객명}")
            .messageDeliveryStatus(MessageDeliveryStatus.FAIL)
            .build();

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("07000000000")
            .canAlimtalk(true)
            .point(1000L)
            .build();

    when(smsRepository.findByMessageIdAndShopId(1L, shopId)).thenReturn(Optional.of(sms));
    when(messageSettingRepository.findByShopId(shopId)).thenReturn(Optional.of(settings));
    when(customerQueryService.getCustomerPhoneNumbers(List.of(1L)))
        .thenReturn(List.of("01011112222"));
    when(messageVariableProcessor.buildPayload(anyLong(), anyLong(), isNull()))
        .thenReturn(Map.of("고객명", "재전송"));
    when(messageVariableProcessor.resolveVariables(any(), any())).thenReturn("실패된 메시지 재전송");

    MessageSendResult successResult = new MessageSendResult(true, "성공", 1L);
    when(coolSmsClient.sendMany(any(), any(), anyList())).thenReturn(List.of(successResult));

    MessageSendResult result = messageCommandService.resendFailedMessage(shopId, 1L);

    assertThat(result.success()).isTrue();
    verify(smsRepository).findByMessageIdAndShopId(1L, shopId);
  }

  @Test
  void markSmsAsFailed_success() {
    List<Long> ids = List.of(1L, 2L);
    List<Sms> messages = List.of(mock(Sms.class), mock(Sms.class));
    when(smsRepository.findAllById(ids)).thenReturn(messages);

    messageCommandService.markSmsAsFailed(ids);

    messages.forEach(m -> verify(m).markAsFailed());
    verify(smsRepository).saveAll(messages);
  }

  @Test
  void markSmsAsSent_success() {
    List<Long> ids = List.of(1L, 2L);
    List<Sms> messages = List.of(mock(Sms.class), mock(Sms.class));
    when(smsRepository.findAllById(ids)).thenReturn(messages);

    messageCommandService.markSmsAsSent(ids);

    messages.forEach(m -> verify(m).markAsSent());
    verify(smsRepository).saveAll(messages);
  }
}
