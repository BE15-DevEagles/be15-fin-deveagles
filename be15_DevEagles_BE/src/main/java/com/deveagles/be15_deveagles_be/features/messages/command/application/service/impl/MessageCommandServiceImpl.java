package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.SmsSendUnit;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.UpdateReservationRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSendResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageClickService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageCommandService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageVariableProcessor;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageDeliveryStatus;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSendingType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSettings;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.Sms;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageSettingRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.SmsRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.infrastructure.CoolSmsClient;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageCommandServiceImpl implements MessageCommandService {
  private final ShopCommandService shopCommandService;
  private final CustomerQueryService customerQueryService;
  private final MessageSettingRepository messageSettingRepository;
  private final CoolSmsClient coolSmsClient;
  private final SmsRepository smsRepository;
  private final MessageVariableProcessor messageVariableProcessor;
  private final MessageClickService messageClickService;

  @Override
  @Transactional
  public List<MessageSendResult> sendSms(Long shopId, SmsRequest smsRequest) {
    shopCommandService.validateShopExists(shopId);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime scheduledAt = resolveScheduledAt(smsRequest, now);
    boolean isReservation = MessageSendingType.RESERVATION.equals(smsRequest.messageSendingType());

    // 1. 고객 ID 원본 리스트
    List<Long> customerIds = smsRequest.customerIds();

    // 2. 중복 제거된 ID로 전화번호 조회 (성공/실패 여부 판단용)
    List<Long> distinctCustomerIds = customerIds.stream().distinct().toList();
    List<String> phoneNumbers = customerQueryService.getCustomerPhoneNumbers(distinctCustomerIds);

    // 3. 메시지 설정 조회 (발신번호 등)
    MessageSettings settings =
        messageSettingRepository
            .findByShopId(shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_SETTINGS_NOT_FOUND));
    String senderNumber = settings.getSenderNumber();

    // 4. Sms 리스트 생성 (치환 포함)
    List<Sms> smsList =
        IntStream.range(0, distinctCustomerIds.size())
            .mapToObj(
                i -> {
                  Long customerId = distinctCustomerIds.get(i);

                  Map<String, String> payload =
                      Optional.ofNullable(smsRequest.payload())
                          .orElseGet(
                              () ->
                                  messageVariableProcessor.buildPayload(customerId, shopId, null));

                  String resolvedContent =
                      messageVariableProcessor.resolveVariables(
                          smsRequest.messageContent(), payload);

                  // ✅ 자동 판단: #{프로필링크} 포함 여부로 hasLink 결정
                  boolean hasLink = resolvedContent != null && resolvedContent.contains("#{프로필링크}");

                  Sms.SmsBuilder builder =
                      Sms.builder()
                          .shopId(shopId)
                          .customerId(customerId)
                          .messageContent(resolvedContent)
                          .messageKind(smsRequest.messageKind())
                          .messageType(smsRequest.messageType())
                          .messageSendingType(smsRequest.messageSendingType())
                          .messageDeliveryStatus(
                              isReservation
                                  ? MessageDeliveryStatus.PENDING
                                  : MessageDeliveryStatus.SENT)
                          .scheduledAt(scheduledAt)
                          .templateId(smsRequest.templateId())
                          .hasLink(hasLink)
                          .customerGradeId(smsRequest.customerGradeId())
                          .tagId(smsRequest.tagId())
                          .couponId(smsRequest.couponId())
                          .workflowId(smsRequest.workflowId());

                  if (!isReservation) {
                    builder.sentAt(now);
                  }

                  return builder.build();
                })
            .toList();

    // 5. 저장
    List<Sms> saved = smsRepository.saveAll(smsList);
    smsRepository.flush();

    // 6. 링크 생성 후 콘텐츠 업데이트
    for (Sms sms : saved) {
      String content = sms.getMessageContent();
      if (Boolean.TRUE.equals(sms.getHasLink())
          && content != null
          && content.contains("#{프로필링크}")) {
        String originalUrl = "http://localhost:5173/p/" + shopId;
        String trackableUrl =
            messageClickService.createTrackableLink(sms.getMessageId(), originalUrl);
        sms.updateContent(content.replace("#{프로필링크}", "\n" + trackableUrl + "\n"));
      }
    }

    // 7. 즉시 발송이면 CoolSMS 호출
    if (!isReservation) {
      List<SmsSendUnit> units =
          IntStream.range(0, saved.size())
              .mapToObj(i -> new SmsSendUnit(saved.get(i).getMessageId(), phoneNumbers.get(i)))
              .toList();

      List<MessageSendResult> results =
          coolSmsClient.sendMany(senderNumber, saved.get(0).getMessageContent(), units);

      List<Long> failedIds =
          results.stream().filter(r -> !r.success()).map(MessageSendResult::messageId).toList();

      if (!failedIds.isEmpty()) {
        markSmsAsFailed(failedIds);
      }

      return results;
    }

    // 예약 발송이면 결과 생성하여 반환
    return saved.stream()
        .map(s -> new MessageSendResult(true, "예약 등록 완료", s.getMessageId()))
        .toList();
  }

  @Override
  @Transactional
  public MessageSendResult resendFailedMessage(Long shopId, Long messageId) {
    shopCommandService.validateShopExists(shopId);

    Sms sms =
        smsRepository
            .findByMessageIdAndShopId(messageId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SMS_NOT_FOUND));

    if (sms.getMessageDeliveryStatus() != MessageDeliveryStatus.FAIL) {
      throw new BusinessException(ErrorCode.INVALID_MESSAGE_RESEND_CONDITION);
    }

    MessageSettings settings =
        messageSettingRepository
            .findByShopId(shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_SETTINGS_NOT_FOUND));
    String senderNumber = settings.getSenderNumber();

    List<String> phoneNumbers =
        customerQueryService.getCustomerPhoneNumbers(List.of(sms.getCustomerId()));
    String phoneNumber = phoneNumbers.get(0);

    // ✅ payload: 고객명만 포함되도록 기본값 생성
    Map<String, String> payload =
        messageVariableProcessor.buildPayload(sms.getCustomerId(), shopId, null);
    String resolvedContent =
        messageVariableProcessor.resolveVariables(sms.getMessageContent(), payload);

    SmsSendUnit unit = new SmsSendUnit(sms.getMessageId(), phoneNumber);
    List<MessageSendResult> results =
        coolSmsClient.sendMany(senderNumber, resolvedContent, List.of(unit));
    MessageSendResult result = results.get(0);

    if (result.success()) {
      markSmsAsSent(List.of(sms.getMessageId()));
    } else {
      markSmsAsFailed(List.of(sms.getMessageId()));
    }

    return result;
  }

  @Override
  @Transactional
  public void updateReservationMessage(
      UpdateReservationRequest updateReservationRequest, Long shopId, Long messageId) {
    shopCommandService.validateShopExists(shopId);
    Sms sms =
        smsRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SMS_NOT_FOUND));
    if (!sms.getShopId().equals(shopId)) {
      throw new BusinessException(ErrorCode.SMS_SHOP_MISMATCH);
    }
    if (sms.getScheduledAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.ALREADY_SENT_OR_CANCELED);
    }
    if (updateReservationRequest.scheduledAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.INVALID_SCHEDULED_TIME);
    }
    sms.updateReservation(
        updateReservationRequest.messageContent(),
        updateReservationRequest.messageKind(),
        updateReservationRequest.customerId(),
        updateReservationRequest.scheduledAt());
  }

  @Override
  @Transactional
  public void cancelScheduledMessage(Long messageId, Long shopId) {
    shopCommandService.validateShopExists(shopId);
    Sms sms =
        smsRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SMS_NOT_FOUND));

    if (!sms.getShopId().equals(shopId)) {
      throw new BusinessException(ErrorCode.SMS_SHOP_MISMATCH);
    }

    if (!sms.isReservable()) {
      throw new BusinessException(ErrorCode.INVALID_MESSAGE_CANCEL_CONDITION);
    }

    sms.cancel();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markSmsAsFailed(Collection<Long> smsIds) {
    List<Sms> failedMessages = smsRepository.findAllById(smsIds);
    failedMessages.forEach(Sms::markAsFailed);
    smsRepository.saveAll(failedMessages);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markSmsAsSent(Collection<Long> smsIds) {
    List<Sms> sentMessages = smsRepository.findAllById(smsIds);
    sentMessages.forEach(Sms::markAsSent);
    smsRepository.saveAll(sentMessages);
  }

  private LocalDateTime resolveScheduledAt(SmsRequest request, LocalDateTime now) {
    if (request.messageSendingType() == MessageSendingType.RESERVATION
        && request.scheduledAt() == null) {
      throw new BusinessException(ErrorCode.SCHEDULE_TIME_REQUIRED_FOR_RESERVATION);
    }

    if (request.messageSendingType() == MessageSendingType.IMMEDIATE
        && request.scheduledAt() != null) {
      throw new BusinessException(ErrorCode.SCHEDULE_TIME_NOT_ALLOWED_FOR_IMMEDIATE);
    }

    return request.messageSendingType() == MessageSendingType.IMMEDIATE
        ? now
        : request.scheduledAt();
  }
}
