package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageVariableProcessor {

  private final CustomerQueryService customerQueryService;

  public Map<String, String> buildPayload(
      Long customerId, Long shopId, Map<String, String> externalPayload) {
    CustomerDetailResponse customer =
        customerQueryService
            .getCustomerDetail(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    Map<String, String> payload = new HashMap<>();
    payload.put("고객명", customer.getCustomerName());

    // 외부에서 예약일, 취소일 등 들어오면 그걸 merge해서 사용
    if (externalPayload != null) {
      payload.putAll(externalPayload);
    }

    return payload;
  }

  // ✅ 템플릿 메시지 치환 - payload 주어졌을 때 바로 치환
  public String resolveVariables(String content, Map<String, String> payload) {
    if (content == null || payload == null) return content;

    System.out.println("🔍 템플릿 content = " + content);
    for (Map.Entry<String, String> entry : payload.entrySet()) {
      String key = "#{" + entry.getKey() + "}";
      String value = Optional.ofNullable(entry.getValue()).orElse("");
      System.out.println("→ 바꾸려는 key = '" + key + "'");
      System.out.println("→ 바꿀 값 = '" + value + "'");
      content = content.replace(key, value);
    }
    System.out.println("✅ 최종 content = " + content);
    return content;
  }

  private String format(LocalDateTime time) {
    return time != null ? time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) : "";
  }
}
