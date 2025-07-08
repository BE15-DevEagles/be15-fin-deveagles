package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerCommandService;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerTagService;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CustomerCommandServiceImpl implements CustomerCommandService {

  private final CustomerRepository customerRepository;
  private final CustomerQueryService customerQueryService;
  private final AutomaticMessageTriggerService automaticMessageTriggerService;
  private final CustomerTagService customerTagService;

  @Override
  public CustomerCommandResponse createCustomer(CreateCustomerRequest request) {
    Customer customer =
        Customer.builder()
            .shopId(request.shopId())
            .customerName(request.customerName())
            .phoneNumber(request.phoneNumber())
            .gender(request.gender())
            .birthdate(request.birthdate())
            .customerGradeId(request.customerGradeId())
            .staffId(request.staffId())
            .channelId(request.channelId())
            .memo(request.memo())
            .marketingConsent(request.marketingConsent())
            .notificationConsent(request.notificationConsent())
            .build();

    Customer savedCustomer = customerRepository.save(customer);
    customerQueryService.syncCustomerToElasticsearch(savedCustomer.getId());

    if (request.tags() != null && !request.tags().isEmpty()) {
      request
          .tags()
          .forEach(
              tagId ->
                  customerTagService.addTagToCustomer(
                      savedCustomer.getId(), tagId, request.shopId()));
    }

    log.info(
        "자동 발송 메시지 실행 - 이벤트: {}, 고객ID: {}", AutomaticEventType.NEW_CUSTOMER, savedCustomer.getId());
    automaticMessageTriggerService.triggerAutomaticSend(
        savedCustomer, AutomaticEventType.NEW_CUSTOMER);

    return CustomerCommandResponse.from(savedCustomer);
  }

  @Override
  public CustomerCommandResponse updateCustomer(UpdateCustomerRequest request) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(request.customerId(), getCurrentShopId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.updateCustomerInfo(
        request.customerName(),
        request.phoneNumber(),
        request.memo(),
        request.gender(),
        request.channelId());

    Customer updatedCustomer = customerRepository.save(customer);

    // Elasticsearch 동기화
    customerQueryService.syncCustomerToElasticsearch(updatedCustomer.getId());

    log.info("고객 정보 수정됨: ID={}, 이름={}", updatedCustomer.getId(), updatedCustomer.getCustomerName());

    return CustomerCommandResponse.from(updatedCustomer);
  }

  @Override
  public void deleteCustomer(Long customerId, Long shopId) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.softDelete();
    customerRepository.save(customer);
    log.info("고객 삭제됨: ID={}, 매장ID={}", customerId, shopId);

    // Elasticsearch 동기화
    customerQueryService.syncCustomerToElasticsearch(customerId);
  }

  @Override
  public CustomerCommandResponse updateMarketingConsent(
      Long customerId, Long shopId, Boolean consent) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.updateMarketingConsent(consent);
    Customer updatedCustomer = customerRepository.save(customer);

    return CustomerCommandResponse.from(updatedCustomer);
  }

  @Override
  public CustomerCommandResponse updateNotificationConsent(
      Long customerId, Long shopId, Boolean consent) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.updateNotificationConsent(consent);
    Customer updatedCustomer = customerRepository.save(customer);

    return CustomerCommandResponse.from(updatedCustomer);
  }

  @Override
  public CustomerCommandResponse addVisit(Long customerId, Long shopId, Integer revenue) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.addVisit(revenue);
    Customer updatedCustomer = customerRepository.save(customer);
    log.info("고객 방문 추가됨: ID={}, 매출={}", customerId, revenue);

    return CustomerCommandResponse.from(updatedCustomer);
  }

  @Override
  public CustomerCommandResponse addNoshow(Long customerId, Long shopId) {
    Customer customer =
        customerRepository
            .findByIdAndShopId(customerId, shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    customer.addNoshow();
    Customer updatedCustomer = customerRepository.save(customer);
    log.info("고객 노쇼 추가됨: ID={}", customerId);

    return CustomerCommandResponse.from(updatedCustomer);
  }

  // TODO: 실제 구현에서는 SecurityContext에서 shopId 가져오기
  private Long getCurrentShopId() {
    return 1L; // 임시값
  }
}
