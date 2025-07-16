package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository.CustomerJpaRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("고객 커맨드 서비스 테스트")
class CustomerCommandServiceImplTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private CustomerQueryService customerQueryService;
  @Mock private CustomerJpaRepository customerJpaRepository;
  @Mock private AutomaticMessageTriggerService automaticMessageTriggerService;
  @InjectMocks private CustomerCommandServiceImpl customerCommandService;

  @BeforeEach
  void setUpSecurityContext() {
    CustomUser principal =
        CustomUser.builder()
            .shopId(1L)
            .userId(1L)
            .username("test")
            .password("pw")
            .staffStatus(null)
            .staffName("test")
            .grade("A")
            .profileUrl(null)
            .authorities(Collections.emptyList())
            .build();
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("고객 생성 테스트")
  class CreateCustomerTest {
    private CreateCustomerRequest request;
    private Customer customer;

    @BeforeEach
    void setUp() {
      request =
          new CreateCustomerRequest(
              1L, // customerGradeId
              1L, // staffId
              "Test Customer",
              "01012345678",
              "Test memo",
              LocalDate.of(1990, 1, 1), // birthdate
              Customer.Gender.M,
              true, // marketingConsent
              true, // notificationConsent
              1L, // channelId
              new ArrayList<>() // tags
              );

      customer =
          Customer.builder()
              .id(1L)
              .customerName(request.customerName())
              .phoneNumber(request.phoneNumber())
              .memo(request.memo())
              .gender(request.gender())
              .channelId(request.channelId())
              .shopId(1L)
              .staffId(request.staffId())
              .customerGradeId(request.customerGradeId())
              .birthdate(request.birthdate())
              .marketingConsent(request.marketingConsent())
              .notificationConsent(request.notificationConsent())
              .build();
    }

    @Test
    @DisplayName("정상적인 고객 생성")
    void createCustomer_Success() {
      // Given
      when(customerRepository.save(any(Customer.class))).thenReturn(customer);
      when(customerQueryService.getCustomerDetail(anyLong(), anyLong()))
          .thenReturn(Optional.of(CustomerDetailResponse.builder().customerId(1L).build()));

      // When
      CustomerCommandResponse response = customerCommandService.createCustomer(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.customerId()).isEqualTo(customer.getId());
      assertThat(response.customerName()).isEqualTo(request.customerName());
      assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
      assertThat(response.memo()).isEqualTo(request.memo());
      assertThat(response.gender()).isEqualTo(request.gender());
      assertThat(response.channelId()).isEqualTo(request.channelId());
      assertThat(response.shopId()).isEqualTo(1L);
      assertThat(response.staffId()).isEqualTo(request.staffId());
      assertThat(response.customerGradeId()).isEqualTo(request.customerGradeId());
      assertThat(response.birthdate()).isEqualTo(request.birthdate());
      assertThat(response.marketingConsent()).isEqualTo(request.marketingConsent());
      assertThat(response.notificationConsent()).isEqualTo(request.notificationConsent());

      then(customerRepository).should().save(any(Customer.class));
      verify(automaticMessageTriggerService)
          .triggerAutomaticSend(any(), eq(AutomaticEventType.NEW_CUSTOMER), isNull());
    }

    @Test
    @DisplayName("중복 전화번호여도 고객이 정상적으로 생성된다")
    void createCustomer_DuplicatePhoneNumberAllowed() {
      // given
      CreateCustomerRequest request =
          new CreateCustomerRequest(
              1L,
              1L,
              "홍길동",
              "01012345678",
              "테스트 고객",
              LocalDate.of(1990, 1, 1),
              Customer.Gender.M,
              false,
              false,
              1L,
              new ArrayList<>());
      // 중복 전화번호가 이미 있다고 가정
      // when(customerRepository.existsByPhoneNumberAndShopId(request.phoneNumber(),
      // 1L)).thenReturn(true);
      Customer customer =
          Customer.builder()
              .id(1L)
              .customerName(request.customerName())
              .phoneNumber(request.phoneNumber())
              .memo(request.memo())
              .gender(request.gender())
              .channelId(request.channelId())
              .shopId(1L)
              .staffId(request.staffId())
              .customerGradeId(request.customerGradeId())
              .birthdate(request.birthdate())
              .marketingConsent(request.marketingConsent())
              .notificationConsent(request.notificationConsent())
              .build();
      when(customerRepository.save(any(Customer.class))).thenReturn(customer);
      when(customerQueryService.getCustomerDetail(anyLong(), anyLong()))
          .thenReturn(Optional.of(CustomerDetailResponse.builder().customerId(1L).build()));

      // when
      CustomerCommandResponse response = customerCommandService.createCustomer(request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.customerName()).isEqualTo(request.customerName());
      assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
      assertThat(response.memo()).isEqualTo(request.memo());
      assertThat(response.gender()).isEqualTo(request.gender());
      assertThat(response.channelId()).isEqualTo(request.channelId());
      assertThat(response.shopId()).isEqualTo(1L);
      assertThat(response.staffId()).isEqualTo(request.staffId());
      assertThat(response.customerGradeId()).isEqualTo(request.customerGradeId());
      assertThat(response.birthdate()).isEqualTo(request.birthdate());
      assertThat(response.marketingConsent()).isEqualTo(request.marketingConsent());
      assertThat(response.notificationConsent()).isEqualTo(request.notificationConsent());
    }
  }

  @Nested
  @DisplayName("고객 정보 수정 테스트")
  class UpdateCustomerTest {
    private UpdateCustomerRequest request;
    private Customer existingCustomer;
    private Customer updatedCustomer;

    @BeforeEach
    void setUp() {
      request =
          new UpdateCustomerRequest(
              1L, // customerId
              "Updated Customer",
              "01087654321",
              "Updated memo",
              Customer.Gender.F,
              2L, // channelId
              2L, // staffId
              2L, // customerGradeId
              LocalDate.of(1995, 1, 1), // birthdate
              false, // marketingConsent
              false // notificationConsent
              );

      existingCustomer =
          Customer.builder()
              .id(1L)
              .customerName("Test Customer")
              .phoneNumber("01012345678")
              .memo("Test memo")
              .gender(Customer.Gender.M)
              .channelId(1L)
              .shopId(1L)
              .staffId(1L)
              .customerGradeId(1L)
              .birthdate(LocalDate.of(1990, 1, 1))
              .marketingConsent(true)
              .notificationConsent(true)
              .build();

      updatedCustomer =
          Customer.builder()
              .id(1L)
              .customerName(request.customerName())
              .phoneNumber(request.phoneNumber())
              .memo(request.memo())
              .gender(request.gender())
              .channelId(request.channelId())
              .shopId(1L)
              .staffId(request.staffId())
              .customerGradeId(request.customerGradeId())
              .birthdate(request.birthdate())
              .marketingConsent(request.marketingConsent())
              .notificationConsent(request.notificationConsent())
              .build();
    }

    @Test
    @DisplayName("정상적인 고객 정보 수정")
    void updateCustomer_Success() {
      // Given
      when(customerRepository.findByIdAndShopId(request.customerId(), 1L))
          .thenReturn(Optional.of(existingCustomer));
      when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

      // When
      CustomerCommandResponse response = customerCommandService.updateCustomer(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.customerId()).isEqualTo(request.customerId());
      assertThat(response.customerName()).isEqualTo(request.customerName());
      assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
      assertThat(response.memo()).isEqualTo(request.memo());
      assertThat(response.gender()).isEqualTo(request.gender());
      assertThat(response.channelId()).isEqualTo(request.channelId());
      assertThat(response.shopId()).isEqualTo(1L);
      assertThat(response.staffId()).isEqualTo(request.staffId());
      assertThat(response.customerGradeId()).isEqualTo(request.customerGradeId());
      assertThat(response.birthdate()).isEqualTo(request.birthdate());
      assertThat(response.marketingConsent()).isEqualTo(request.marketingConsent());
      assertThat(response.notificationConsent()).isEqualTo(request.notificationConsent());

      then(customerRepository).should().findByIdAndShopId(request.customerId(), 1L);
      then(customerRepository).should().save(any(Customer.class));
    }
  }

  @Test
  @DisplayName("고객 정보 수정 실패 - 존재하지 않는 고객")
  void updateCustomer_CustomerNotFound() {
    // given
    Long customerId = 1L;
    UpdateCustomerRequest request =
        new UpdateCustomerRequest(
            customerId,
            "홍길순",
            "01098765432",
            "수정된 메모",
            Customer.Gender.F,
            2L,
            2L,
            2L,
            LocalDate.of(1990, 2, 2),
            true,
            true);

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
    assertThat(existingCustomer.isDeleted()).isTrue();
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
    assertThat(existingCustomer.getMarketingConsent()).isTrue();
    assertThat(existingCustomer.getMarketingConsentedAt()).isNotNull();

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
    assertThat(existingCustomer.getNotificationConsent()).isTrue();

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
    Integer initialVisitCount = existingCustomer.getVisitCount();
    Integer initialRevenue = existingCustomer.getTotalRevenue();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.addVisit(customerId, shopId, revenue);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);
    assertThat(existingCustomer.getVisitCount()).isEqualTo(initialVisitCount + 1);
    assertThat(existingCustomer.getTotalRevenue()).isEqualTo(initialRevenue + revenue);
    assertThat(existingCustomer.getRecentVisitDate()).isEqualTo(LocalDate.now());

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
    Integer initialNoshowCount = existingCustomer.getNoshowCount();

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(customerRepository.save(any(Customer.class))).willReturn(existingCustomer);

    // when
    CustomerCommandResponse response = customerCommandService.addNoshow(customerId, shopId);

    // then
    assertThat(response.customerId()).isEqualTo(customerId);
    assertThat(existingCustomer.getNoshowCount()).isEqualTo(initialNoshowCount + 1);

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
    assertThatThrownBy(() -> customerCommandService.deleteCustomer(customerId, shopId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

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

    assertThatThrownBy(() -> customerCommandService.addVisit(customerId, shopId, 50000))
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
        .noshowCount(0)
        .gender(Customer.Gender.M)
        .marketingConsent(false)
        .notificationConsent(false)
        .channelId(1L)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("createUnknownCustomer -  미등록 고객 정보가 저장된다")
  void createUnknownCustomer_성공() {
    // given
    Long shopId = 1L;

    Object GenderType;
    CreateCustomerRequest request =
        new CreateCustomerRequest(
            3L,
            2L,
            "미등록-여자",
            "01000000000",
            null, // channelId
            LocalDate.now(), // marketingConsent
            Customer.Gender.F, // notificationConsent
            false,
            false,
            5L,
            null);

    ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);

    // when
    customerCommandService.createUnknownCustomer(shopId, request);

    // then
    verify(customerRepository).save(customerCaptor.capture());

    Customer savedCustomer = customerCaptor.getValue();
    assertEquals(shopId, savedCustomer.getShopId());
    assertEquals(request.customerGradeId(), savedCustomer.getCustomerGradeId());
    assertEquals(request.customerName(), savedCustomer.getCustomerName());
    assertEquals(request.gender(), savedCustomer.getGender());
    assertEquals(request.birthdate(), savedCustomer.getBirthdate());
    assertEquals(request.channelId(), savedCustomer.getChannelId());
    assertEquals(request.marketingConsent(), savedCustomer.getMarketingConsent());
    assertEquals(request.notificationConsent(), savedCustomer.getNotificationConsent());
    assertEquals(request.phoneNumber(), savedCustomer.getPhoneNumber());
    assertEquals(request.memo(), savedCustomer.getMemo());
    assertEquals(request.staffId(), savedCustomer.getStaffId());
  }
}
