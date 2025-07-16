package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerCommandService;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerTagService;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
    Long currentShopId = getCurrentShopId();

    Customer customer =
        Customer.builder()
            .shopId(currentShopId)
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
                  customerTagService.addTagToCustomer(savedCustomer.getId(), tagId, currentShopId));
    }

    CustomerDetailResponse customerDto =
        customerQueryService
            .getCustomerDetail(savedCustomer.getId(), currentShopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    automaticMessageTriggerService.triggerAutomaticSend(
        customerDto, AutomaticEventType.NEW_CUSTOMER, null);

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

    // 추가 필드 업데이트
    if (request.staffId() != null) {
      customer.updateStaff(request.staffId());
    }
    if (request.customerGradeId() != null) {
      customer.updateGrade(request.customerGradeId());
    }
    if (request.birthdate() != null) {
      customer.updateBirthdate(request.birthdate());
    }
    if (request.marketingConsent() != null) {
      customer.updateMarketingConsent(request.marketingConsent());
    }
    if (request.notificationConsent() != null) {
      customer.updateNotificationConsent(request.notificationConsent());
    }

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

  @Override
  public void createUnknownCustomer(Long shopId, CreateCustomerRequest request) {

    Customer customer =
        Customer.builder()
            .shopId(shopId)
            .customerGradeId(request.customerGradeId())
            .customerName(request.customerName())
            .gender(request.gender())
            .birthdate(request.birthdate())
            .channelId(request.channelId())
            .marketingConsent(request.marketingConsent())
            .notificationConsent(request.notificationConsent())
            .phoneNumber(request.phoneNumber())
            .memo(request.memo())
            .staffId(request.staffId())
            .build();

    customerRepository.save(customer);
  }

  // SecurityContext에서 현재 사용자의 shopId 가져오기
  private Long getCurrentShopId() {
    CustomUser user =
        (CustomUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getShopId();
  }
}
