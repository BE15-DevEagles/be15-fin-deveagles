package com.deveagles.be15_deveagles_be.features.workflows.execution.infrastructure.service;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.SmsRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageCommandService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageKind;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSendingType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageType;
import com.deveagles.be15_deveagles_be.features.messages.query.service.MessageTemplateQueryService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceAdapter {

  private final MessageCommandService messageCommandService;
  private final MessageTemplateQueryService messageTemplateQueryService;
  private final CustomerQueryService customerQueryService;

  public boolean sendMessage(Long customerId, Long shopId, String templateId, LocalTime sendTime) {
    try {
      // 고객 정보 조회
      Optional<CustomerDetailResponse> customerOpt =
          customerQueryService.getCustomerDetail(customerId, shopId);
      if (customerOpt.isEmpty()) {
        log.warn("고객을 찾을 수 없습니다. customerId: {}", customerId);
        return false;
      }
      CustomerDetailResponse customer = customerOpt.get();

      // 템플릿 조회
      Long tId = Long.parseLong(templateId);
      var template = messageTemplateQueryService.findOne(shopId, tId);
      if (template == null) {
        log.warn("메시지 템플릿을 찾을 수 없습니다. templateId: {}, shopId: {}", templateId, shopId);
        return false;
      }

      // 발송 시간에 따른 발송 타입 결정
      MessageSendingType sendingType =
          sendTime != null ? MessageSendingType.RESERVATION : MessageSendingType.IMMEDIATE;

      // 예약 발송인 경우 오늘 날짜의 지정된 시간으로 설정
      LocalDateTime scheduledTime =
          sendTime != null
              ? LocalDateTime.of(LocalDateTime.now().toLocalDate(), sendTime)
              : LocalDateTime.now();

      log.info(
          "메시지 발송 설정 - customerId: {}, sendingType: {}, scheduledTime: {}",
          customerId,
          sendingType,
          scheduledTime);

      // SMS 요청 생성
      SmsRequest smsRequest =
          new SmsRequest(
              List.of(customerId),
              template.getTemplateContent(),
              MessageType.SMS,
              sendingType,
              scheduledTime,
              tId,
              false,
              null,
              null,
              MessageKind.advertising,
              null,
              null,
              null);

      // 메시지 발송
      var results = messageCommandService.sendSms(shopId, smsRequest);
      return !results.isEmpty() && results.get(0).success();

    } catch (Exception e) {
      log.error(
          "메시지 발송 실패 - customerId: {}, templateId: {}, shopId: {}",
          customerId,
          templateId,
          shopId,
          e);
      return false;
    }
  }

  public boolean sendCouponMessage(
      Long customerId, Long shopId, String templateId, String couponCode, LocalTime sendTime) {
    try {
      // 고객 정보 조회
      Optional<CustomerDetailResponse> customerOpt =
          customerQueryService.getCustomerDetail(customerId, shopId);
      if (customerOpt.isEmpty()) {
        log.warn("고객을 찾을 수 없습니다. customerId: {}", customerId);
        return false;
      }
      CustomerDetailResponse customer = customerOpt.get();

      // 템플릿 조회
      Long tId = Long.parseLong(templateId);
      var template = messageTemplateQueryService.findOne(shopId, tId);
      if (template == null) {
        log.warn("메시지 템플릿을 찾을 수 없습니다. templateId: {}, shopId: {}", templateId, shopId);
        return false;
      }

      // 템플릿 내용에 쿠폰 정보 추가
      String content = template.getTemplateContent();
      if (couponCode != null) {
        content = content.replace("{{couponCode}}", couponCode);
      }

      // 발송 시간에 따른 발송 타입 결정
      MessageSendingType sendingType =
          sendTime != null ? MessageSendingType.RESERVATION : MessageSendingType.IMMEDIATE;

      // 예약 발송인 경우 오늘 날짜의 지정된 시간으로 설정
      LocalDateTime scheduledTime =
          sendTime != null
              ? LocalDateTime.of(LocalDateTime.now().toLocalDate(), sendTime)
              : LocalDateTime.now();

      log.info(
          "쿠폰 메시지 발송 설정 - customerId: {}, couponCode: {}, sendingType: {}, scheduledTime: {}",
          customerId,
          couponCode,
          sendingType,
          scheduledTime);

      // SMS 요청 생성
      SmsRequest smsRequest =
          new SmsRequest(
              List.of(customerId),
              content,
              MessageType.SMS,
              sendingType,
              scheduledTime,
              tId,
              false,
              null,
              null,
              MessageKind.advertising,
              null,
              null,
              null);

      // 메시지 발송
      var results = messageCommandService.sendSms(shopId, smsRequest);
      return !results.isEmpty() && results.get(0).success();

    } catch (Exception e) {
      log.error(
          "쿠폰 메시지 발송 실패 - customerId: {}, templateId: {}, couponCode: {}, shopId: {}",
          customerId,
          templateId,
          couponCode,
          shopId,
          e);
      return false;
    }
  }
}
