package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.SmsSendUnit;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSendResult;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageDeliveryStatus;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSendingType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSettings;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.Sms;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageSettingRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.SmsRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.infrastructure.CoolSmsClient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledSmsSender {

  private final SmsRepository smsRepository;
  private final CustomerQueryService customerQueryService;
  private final MessageSettingRepository messageSettingRepository;
  private final CoolSmsClient coolSmsClient;
  private final MessageCommandService messageCommandService;

  @Scheduled(fixedDelay = 60000) // 이전 작업이 끝난 후 60초 뒤에 실행
  public void sendScheduledMessages() {
    log.info("예약 메시지 스케줄러 실행됨 - {}", LocalDateTime.now());

    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

    // 1. 현재 시간에 예약된 메시지 조회
    List<Sms> scheduledMessages =
        smsRepository
            .findAllByMessageSendingTypeAndScheduledAtLessThanEqualAndMessageDeliveryStatus(
                MessageSendingType.RESERVATION, now, MessageDeliveryStatus.PENDING);

    if (scheduledMessages.isEmpty()) return;

    // 2. 고객 전화번호 매핑
    List<Long> customerIds = scheduledMessages.stream().map(Sms::getCustomerId).distinct().toList();
    List<String> phoneNumbers = customerQueryService.getCustomerPhoneNumbers(customerIds);

    Map<Long, String> phoneNumberMap = new HashMap<>();
    for (int i = 0; i < customerIds.size(); i++) {
      phoneNumberMap.put(customerIds.get(i), phoneNumbers.get(i));
    }

    // 3. shopId → senderNumber 캐싱 처리
    Map<Long, String> senderNumberMap = new HashMap<>();
    for (Sms sms : scheduledMessages) {
      senderNumberMap.computeIfAbsent(
          sms.getShopId(),
          shopId ->
              messageSettingRepository
                  .findByShopId(shopId)
                  .map(MessageSettings::getSenderNumber)
                  .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_SETTINGS_NOT_FOUND)));
    }

    // 4. senderNumber + messageContent 로 그룹핑
    Map<String, List<Sms>> grouped =
        scheduledMessages.stream()
            .filter(sms -> phoneNumberMap.containsKey(sms.getCustomerId()))
            .collect(
                Collectors.groupingBy(
                    sms -> senderNumberMap.get(sms.getShopId()) + "|" + sms.getMessageContent()));

    List<MessageSendResult> allResults = new ArrayList<>();

    // 5. 그룹별 전송
    for (Map.Entry<String, List<Sms>> entry : grouped.entrySet()) {
      String[] keyParts = entry.getKey().split("\\|", 2);
      String senderNumber = keyParts[0];
      String messageContent = keyParts[1];
      List<Sms> smsGroup = entry.getValue();

      List<SmsSendUnit> units =
          smsGroup.stream()
              .map(
                  sms ->
                      new SmsSendUnit(sms.getMessageId(), phoneNumberMap.get(sms.getCustomerId())))
              .toList();

      List<MessageSendResult> results = coolSmsClient.sendMany(senderNumber, messageContent, units);
      allResults.addAll(results);
    }

    // 6. 성공/실패 반영
    List<Long> successIds =
        allResults.stream()
            .filter(MessageSendResult::success)
            .map(MessageSendResult::messageId)
            .toList();

    List<Long> failedIds =
        allResults.stream().filter(r -> !r.success()).map(MessageSendResult::messageId).toList();

    if (!successIds.isEmpty()) {
      messageCommandService.markSmsAsSent(successIds);
    }

    if (!failedIds.isEmpty()) {
      messageCommandService.markSmsAsFailed(failedIds);
    }
  }
}
