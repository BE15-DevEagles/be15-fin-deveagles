package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.TagByCustomer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.TagByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.TagRepository;
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
@DisplayName("고객 태그 서비스 테스트")
class CustomerTagServiceImplTest {

  @Mock private TagByCustomerRepository tagByCustomerRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private TagRepository tagRepository;

  @InjectMocks private CustomerTagServiceImpl customerTagService;

  @Test
  @DisplayName("고객에게 태그 추가 성공")
  void addTagToCustomer_Success() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    Customer existingCustomer = createTestCustomer(customerId, shopId);

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(tagRepository.existsById(tagId)).willReturn(true);
    given(tagByCustomerRepository.existsByCustomerIdAndTagId(customerId, tagId)).willReturn(false);

    // when
    customerTagService.addTagToCustomer(customerId, tagId, shopId);

    // then
    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagRepository).should().existsById(tagId);
    then(tagByCustomerRepository).should().existsByCustomerIdAndTagId(customerId, tagId);
    then(tagByCustomerRepository).should().save(any(TagByCustomer.class));
  }

  @Test
  @DisplayName("고객에게 태그 추가 실패 - 존재하지 않는 고객")
  void addTagToCustomer_CustomerNotFound() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    given(customerRepository.findByIdAndShopId(customerId, shopId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerTagService.addTagToCustomer(customerId, tagId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("고객을 찾을 수 없습니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagRepository).should(never()).existsById(any());
    then(tagByCustomerRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("고객에게 태그 추가 실패 - 존재하지 않는 태그")
  void addTagToCustomer_TagNotFound() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    Customer existingCustomer = createTestCustomer(customerId, shopId);

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(tagRepository.existsById(tagId)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> customerTagService.addTagToCustomer(customerId, tagId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("태그를 찾을 수 없습니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagRepository).should().existsById(tagId);
    then(tagByCustomerRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("고객에게 태그 추가 실패 - 이미 할당된 태그")
  void addTagToCustomer_TagAlreadyAssigned() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    Customer existingCustomer = createTestCustomer(customerId, shopId);

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(tagRepository.existsById(tagId)).willReturn(true);
    given(tagByCustomerRepository.existsByCustomerIdAndTagId(customerId, tagId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> customerTagService.addTagToCustomer(customerId, tagId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("이미 고객에게 할당된 태그입니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagRepository).should().existsById(tagId);
    then(tagByCustomerRepository).should().existsByCustomerIdAndTagId(customerId, tagId);
    then(tagByCustomerRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("고객에서 태그 제거 성공")
  void removeTagFromCustomer_Success() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    Customer existingCustomer = createTestCustomer(customerId, shopId);

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(tagByCustomerRepository.existsByCustomerIdAndTagId(customerId, tagId)).willReturn(true);

    // when
    customerTagService.removeTagFromCustomer(customerId, tagId, shopId);

    // then
    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagByCustomerRepository).should().existsByCustomerIdAndTagId(customerId, tagId);
    then(tagByCustomerRepository).should().deleteByCustomerIdAndTagId(customerId, tagId);
  }

  @Test
  @DisplayName("고객에서 태그 제거 실패 - 존재하지 않는 고객")
  void removeTagFromCustomer_CustomerNotFound() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    given(customerRepository.findByIdAndShopId(customerId, shopId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerTagService.removeTagFromCustomer(customerId, tagId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("고객을 찾을 수 없습니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagByCustomerRepository).should(never()).deleteByCustomerIdAndTagId(any(), any());
  }

  @Test
  @DisplayName("고객에서 태그 제거 실패 - 할당되지 않은 태그")
  void removeTagFromCustomer_TagNotAssigned() {
    // given
    Long customerId = 1L;
    Long tagId = 1L;
    Long shopId = 1L;

    Customer existingCustomer = createTestCustomer(customerId, shopId);

    given(customerRepository.findByIdAndShopId(customerId, shopId))
        .willReturn(Optional.of(existingCustomer));
    given(tagByCustomerRepository.existsByCustomerIdAndTagId(customerId, tagId)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> customerTagService.removeTagFromCustomer(customerId, tagId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("고객에게 할당되지 않은 태그입니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
    then(tagByCustomerRepository).should().existsByCustomerIdAndTagId(customerId, tagId);
    then(tagByCustomerRepository).should(never()).deleteByCustomerIdAndTagId(any(), any());
  }

  private Customer createTestCustomer(Long customerId, Long shopId) {
    return Customer.builder()
        .id(customerId)
        .customerGradeId(1L)
        .shopId(shopId)
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
