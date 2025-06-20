package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("고객 커맨드 서비스 테스트")
class CustomerCommandServiceImplTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private CustomerQueryService customerQueryService;

  @InjectMocks private CustomerCommandServiceImpl customerCommandService;

  @Test
  @DisplayName("고객 생성 성공")
  void createCustomer_Success() {
    // given
    CreateCustomerRequest request =
        new CreateCustomerRequest(
            1L, // customerGradeId
            1L, // shopId
            1L, // staffId
            "홍길동",
            "01012345678",
            "테스트 고객",
            LocalDate.of(1990, 1, 1),
            LocalDateTime.now(),
            Customer.Gender.M,
            false,
            false,
            1L // channelId
            );

    Customer savedCustomer = createTestCustomer();

    given(customerRepository.existsByPhoneNumberAndShopId(request.phoneNumber(), request.shopId()))
        .willReturn(false);
    given(customerRepository.save(any(Customer.class))).willReturn(savedCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.createCustomer(request);

    // then
    assertThat(response.customerId()).isEqualTo(savedCustomer.getId());
    assertThat(response.customerName()).isEqualTo(savedCustomer.getCustomerName());
    assertThat(response.phoneNumber()).isEqualTo(savedCustomer.getPhoneNumber());

    then(customerRepository)
        .should()
        .existsByPhoneNumberAndShopId(request.phoneNumber(), request.shopId());
    then(customerRepository).should().save(any(Customer.class));
    then(customerQueryService).should().syncCustomerToElasticsearch(savedCustomer.getId());
  }

  @Test
  @DisplayName("고객 생성 실패 - 중복된 전화번호")
  void createCustomer_DuplicatePhoneNumber() {
    // given
    CreateCustomerRequest request =
        new CreateCustomerRequest(
            1L,
            1L,
            1L,
            "홍길동",
            "01012345678",
            "테스트 고객",
            LocalDate.of(1990, 1, 1),
            LocalDateTime.now(),
            Customer.Gender.M,
            false,
            false,
            1L);

    given(customerRepository.existsByPhoneNumberAndShopId(request.phoneNumber(), request.shopId()))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> customerCommandService.createCustomer(request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_PHONE_DUPLICATE);

    then(customerRepository)
        .should()
        .existsByPhoneNumberAndShopId(request.phoneNumber(), request.shopId());
    then(customerRepository).should(never()).save(any(Customer.class));
  }

  @Test
  @DisplayName("고객 정보 수정 성공")
  void updateCustomer_Success() {
    // given
    Long customerId = 1L;
    UpdateCustomerRequest request =
        new UpdateCustomerRequest(
            customerId, "홍길순", "01098765432", "수정된 메모", Customer.Gender.F, 2L);

    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, 1L))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.updateCustomer(request);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);

    then(customerRepository).should().findByIdAndShopId(customerId, 1L);
    then(customerRepository).should().save(existingCustomer);
    then(customerQueryService).should().syncCustomerToElasticsearch(customerId);
  }

  @Test
  @DisplayName("고객 정보 수정 실패 - 존재하지 않는 고객")
  void updateCustomer_CustomerNotFound() {
    // given
    Long customerId = 1L;
    UpdateCustomerRequest request =
        new UpdateCustomerRequest(
            customerId, "홍길순", "01098765432", "수정된 메모", Customer.Gender.F, 2L);

    given(customerRepository.findByIdAndShopId(customerId, 1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerCommandService.updateCustomer(request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

    then(customerRepository).should().findByIdAndShopId(customerId, 1L);
    then(customerRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("고객 삭제 성공")
  void deleteCustomer_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));

    // when
    customerCommandService.deleteCustomer(customerId, shopId);

    // then
    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(customerRepository).should().save(existingCustomer);
  }

  @Test
  @DisplayName("마케팅 동의 변경 성공")
  void updateMarketingConsent_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    Boolean consent = true;
    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response =
        customerCommandService.updateMarketingConsent(customerId, shopId, consent);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(customerRepository).should().save(existingCustomer);
  }

  @Test
  @DisplayName("알림 동의 변경 성공")
  void updateNotificationConsent_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    Boolean consent = true;
    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response =
        customerCommandService.updateNotificationConsent(customerId, shopId, consent);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(customerRepository).should().save(existingCustomer);
  }

  @Test
  @DisplayName("방문 추가 성공")
  void addVisit_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    Integer revenue = 50000;
    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.addVisit(customerId, shopId, revenue);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(customerRepository).should().save(existingCustomer);
  }

  @Test
  @DisplayName("노쇼 추가 성공")
  void addNoshow_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    Customer existingCustomer = createTestCustomer();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.addNoshow(customerId, shopId);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(customerRepository).should().save(existingCustomer);
  }

  @Test
  @DisplayName("고객 조회 실패시 BusinessException 발생")
  void customerNotFound_ThrowsBusinessException() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;

    given(customerRepository.findByIdAndShopId(customerId, shopId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () -> customerCommandService.updateMarketingConsent(customerId, shopId, true))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

    assertThatThrownBy(
            () -> customerCommandService.updateNotificationConsent(customerId, shopId, true))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

    assertThatThrownBy(() -> customerCommandService.addVisit(customerId, shopId, 10000))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

    assertThatThrownBy(() -> customerCommandService.addNoshow(customerId, shopId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
  }

  private Customer createTestCustomer() {
    return Customer.builder()
        .id(1L)
        .customerGradeId(1L)
        .shopId(1L)
        .staffId(1L)
        .customerName("홍길동")
        .phoneNumber("01012345678")
        .memo("테스트 고객")
        .visitCount(0)
        .totalRevenue(0)
        .recentVisitDate(LocalDate.now())
        .birthdate(LocalDate.of(1990, 1, 1))
        .registeredAt(LocalDateTime.now())
        .noshowCount(0)
        .gender(Customer.Gender.M)
        .marketingConsent(false)
        .notificationConsent(false)
        .channelId(1L)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }
}
