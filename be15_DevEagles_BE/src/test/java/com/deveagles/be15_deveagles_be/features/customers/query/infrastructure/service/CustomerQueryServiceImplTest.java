package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository.CustomerElasticsearchRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository.CustomerJpaRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerDetailQueryRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerListQueryRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("고객 쿼리 서비스 테스트")
class CustomerQueryServiceImplTest {

  @Mock private CustomerJpaRepository customerJpaRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private CustomerElasticsearchRepository elasticsearchRepository;
  @Mock private CustomerDetailQueryRepository customerDetailQueryRepository;
  @Mock private CustomerListQueryRepository customerListQueryRepository;
  @Mock private JPAQueryFactory queryFactory;
  @Mock private JPAQuery<Tuple> jpaQuery;
  @Mock private JPAQuery<String> jpaStringQuery;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentByCustomerRepository segmentByCustomerRepository;

  @InjectMocks private CustomerQueryServiceImpl customerQueryService;

  @Test
  @DisplayName("전화번호로 고객 조회 성공")
  void getCustomerByPhoneNumber_Success() {
    // given
    String phoneNumber = "01012345678";
    Long shopId = 1L;
    Customer customer = createTestCustomer();

    given(customerJpaRepository.findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId))
        .willReturn(Optional.of(customer));

    // when
    Optional<CustomerResponse> response =
        customerQueryService.getCustomerByPhoneNumber(phoneNumber, shopId);

    // then
    assertThat(response).isPresent();
    assertThat(response.get().customerName()).isEqualTo("홍길동");
    assertThat(response.get().phoneNumber()).isEqualTo(phoneNumber);

    then(customerJpaRepository)
        .should()
        .findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Test
  @DisplayName("전화번호로 고객 조회 - 존재하지 않는 고객")
  void getCustomerByPhoneNumber_NotFound() {
    // given
    String phoneNumber = "01012345678";
    Long shopId = 1L;

    given(customerJpaRepository.findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId))
        .willReturn(Optional.empty());

    // when
    Optional<CustomerResponse> response =
        customerQueryService.getCustomerByPhoneNumber(phoneNumber, shopId);

    // then
    assertThat(response).isEmpty();

    then(customerJpaRepository)
        .should()
        .findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Test
  @DisplayName("매장별 고객 수 조회 성공")
  void getCustomerCountByShopId_Success() {
    // given
    Long shopId = 1L;
    long expectedCount = 150L;

    given(customerJpaRepository.countByShopIdAndDeletedAtIsNull(shopId)).willReturn(expectedCount);

    // when
    long count = customerQueryService.getCustomerCountByShopId(shopId);

    // then
    assertThat(count).isEqualTo(expectedCount);

    then(customerJpaRepository).should().countByShopIdAndDeletedAtIsNull(shopId);
  }

  @Test
  @DisplayName("전화번호 중복 확인 - 존재함")
  void existsByPhoneNumber_Exists() {
    // given
    String phoneNumber = "01012345678";
    Long shopId = 1L;

    given(customerJpaRepository.existsByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId))
        .willReturn(true);

    // when
    boolean exists = customerQueryService.existsByPhoneNumber(phoneNumber, shopId);

    // then
    assertThat(exists).isTrue();

    then(customerJpaRepository)
        .should()
        .existsByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Test
  @DisplayName("전화번호 중복 확인 - 존재하지 않음")
  void existsByPhoneNumber_NotExists() {
    // given
    String phoneNumber = "01012345678";
    Long shopId = 1L;

    given(customerJpaRepository.existsByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId))
        .willReturn(false);

    // when
    boolean exists = customerQueryService.existsByPhoneNumber(phoneNumber, shopId);

    // then
    assertThat(exists).isFalse();

    then(customerJpaRepository)
        .should()
        .existsByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Test
  @DisplayName("고객 상세 조회 성공")
  void getCustomerDetail_Success() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;
    CustomerDetailResponse expectedResponse = createTestCustomerDetailResponse();

    given(customerDetailQueryRepository.findCustomerDetailById(customerId, shopId))
        .willReturn(Optional.of(expectedResponse));

    // when
    Optional<CustomerDetailResponse> response =
        customerQueryService.getCustomerDetail(customerId, shopId);

    // then
    assertThat(response).isPresent();
    assertThat(response.get().getCustomerId()).isEqualTo(customerId);
    assertThat(response.get().getCustomerName()).isEqualTo("홍길동");

    then(customerDetailQueryRepository).should().findCustomerDetailById(customerId, shopId);
  }

  @Test
  @DisplayName("고객 목록 조회 성공")
  void getCustomerList_Success() {
    // given
    Long shopId = 1L;
    List<CustomerListResponse> expectedList = List.of(createTestCustomerListResponse());

    given(customerListQueryRepository.findCustomerListByShopId(shopId)).willReturn(expectedList);

    // when
    List<CustomerListResponse> response = customerQueryService.getCustomerList(shopId);

    // then
    assertThat(response).hasSize(1);
    assertThat(response.get(0).getCustomerName()).isEqualTo("홍길동");

    then(customerListQueryRepository).should().findCustomerListByShopId(shopId);
  }

  @Test
  @DisplayName("고객 목록 페이징 조회 성공")
  void getCustomerListPaged_Success() {
    // given
    Long shopId = 1L;
    Pageable pageable = PageRequest.of(0, 10);
    List<CustomerListResponse> customers = List.of(createTestCustomerListResponse());
    Page<CustomerListResponse> expectedPage = new PageImpl<>(customers, pageable, 1);

    given(customerListQueryRepository.findCustomerListByShopId(shopId, pageable))
        .willReturn(expectedPage);

    // when
    Page<CustomerListResponse> response =
        customerQueryService.getCustomerListPaged(shopId, pageable);

    // then
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getTotalElements()).isEqualTo(1);

    then(customerListQueryRepository).should().findCustomerListByShopId(shopId, pageable);
  }

  @Test
  @DisplayName("키워드 검색 성공 - Elasticsearch")
  void searchByKeyword_Success() {
    // given
    String keyword = "홍길동";
    Long shopId = 1L;
    List<CustomerDocument> documents = List.of(createTestCustomerDocument());

    given(elasticsearchRepository.searchByNameOrPhoneNumber(shopId, keyword)).willReturn(documents);

    // when
    List<CustomerSearchResult> results = customerQueryService.searchByKeyword(keyword, shopId);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).customerName()).isEqualTo("홍길동");

    then(elasticsearchRepository).should().searchByNameOrPhoneNumber(shopId, keyword);
  }

  @Test
  @DisplayName("자동완성 검색 성공")
  void autocomplete_Success() {
    // given
    String prefix = "홍";
    Long shopId = 1L;
    List<CustomerDocument> documents = List.of(createTestCustomerDocument());

    given(elasticsearchRepository.autocomplete(shopId, prefix)).willReturn(documents);

    // when
    List<String> suggestions = customerQueryService.autocomplete(prefix, shopId);

    // then
    assertThat(suggestions).hasSize(1);
    assertThat(suggestions.get(0)).contains("홍길동");

    then(elasticsearchRepository).should().autocomplete(shopId, prefix);
  }

  @Test
  @DisplayName("자동완성 검색 실패시 빈 목록 반환")
  void autocomplete_FailureReturnsEmptyList() {
    // given
    String prefix = "홍";
    Long shopId = 1L;

    doThrow(new RuntimeException("Elasticsearch connection failed"))
        .when(elasticsearchRepository)
        .autocomplete(shopId, prefix);

    // when
    List<String> suggestions = customerQueryService.autocomplete(prefix, shopId);

    // then
    assertThat(suggestions).isEmpty();

    then(elasticsearchRepository).should().autocomplete(shopId, prefix);
  }

  @Test
  @DisplayName("고객 태그 조회시 고객 존재하지 않으면 예외 발생")
  void getCustomerTags_CustomerNotFound() {
    // given
    Long customerId = 1L;
    Long shopId = 1L;

    given(customerRepository.findByIdAndShopId(customerId, shopId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerQueryService.getCustomerTags(customerId, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("고객을 찾을 수 없습니다.");

    then(customerRepository).should().findByIdAndShopId(customerId, shopId);
  }

  @Test
  @DisplayName("전화번호로 고객 ID 조회 성공")
  void findCustomerIdByPhoneNumber_Success() {
    // given
    String phoneNumber = "01012345678";
    Long shopId = 1L;
    Customer customer = createTestCustomer();

    given(customerJpaRepository.findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId))
        .willReturn(Optional.of(customer));

    // when
    Optional<CustomerIdResponse> response =
        customerQueryService.findCustomerIdByPhoneNumber(phoneNumber, shopId);

    // then
    assertThat(response).isPresent();
    assertThat(response.get().id()).isEqualTo(customer.getId());

    then(customerJpaRepository)
        .should()
        .findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Test
  @DisplayName("고객 ID 리스트로 전화번호 조회 성공")
  void getCustomerPhoneNumbers_Success() {
    // given
    List<Long> customerIds = List.of(1L, 2L);
    Customer customer1 = createTestCustomer(); // id = 1L, phone = 01012345678
    Customer customer2 =
        Customer.builder()
            .id(2L)
            .shopId(1L)
            .customerName("김영희")
            .phoneNumber("01098765432")
            .createdAt(LocalDateTime.now())
            .modifiedAt(LocalDateTime.now())
            .build();

    List<Customer> customers = List.of(customer1, customer2);

    given(customerJpaRepository.findAllById(customerIds)).willReturn(customers);

    // when
    List<String> phoneNumbers = customerQueryService.getCustomerPhoneNumbers(customerIds);

    // then
    assertThat(phoneNumbers).containsExactlyInAnyOrder("01012345678", "01098765432");

    then(customerJpaRepository).should().findAllById(customerIds);
  }

  @Test
  @DisplayName("고객 ID 리스트로 전화번호 조회 실패 - 일부 고객 없음")
  void getCustomerPhoneNumbers_Failure_CustomerNotFound() {
    // given
    List<Long> customerIds = List.of(1L, 2L);
    Customer customer1 = createTestCustomer(); // id = 1L
    List<Customer> customers = List.of(customer1); // 1명만 반환

    given(customerJpaRepository.findAllById(customerIds)).willReturn(customers);

    // when & then
    assertThatThrownBy(() -> customerQueryService.getCustomerPhoneNumbers(customerIds))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("고객을 찾을 수 없습니다");

    then(customerJpaRepository).should().findAllById(customerIds);
  }

  @Test
  @DisplayName("미등록 고객 목록 조회 성공 - 남자, 여자 모두 조회")
  void getUnregisteredCustomers_Success_BothExist() {
    // given
    Long shopId = 1L;
    List<String> unregisteredNames = Arrays.asList("미등록-남자", "미등록-여자");

    Customer maleCustomer = createUnregisteredCustomer(2L, "미등록-남자", Customer.Gender.M);
    Customer femaleCustomer = createUnregisteredCustomer(3L, "미등록-여자", Customer.Gender.F);

    given(
            customerJpaRepository.findByShopIdAndCustomerNameInAndDeletedAtIsNull(
                shopId, unregisteredNames))
        .willReturn(List.of(maleCustomer, femaleCustomer));

    // when
    List<CustomerResponse> response = customerQueryService.getUnregisteredCustomers(shopId);

    // then
    assertThat(response).hasSize(2);
    assertThat(response)
        .extracting(CustomerResponse::customerName)
        .containsExactlyInAnyOrder("미등록-남자", "미등록-여자");

    then(customerJpaRepository)
        .should()
        .findByShopIdAndCustomerNameInAndDeletedAtIsNull(shopId, unregisteredNames);
  }

  @Test
  @DisplayName("미등록 고객 목록 조회 성공 - 일부만 존재")
  void getUnregisteredCustomers_Success_PartialExist() {
    // given
    Long shopId = 1L;
    List<String> unregisteredNames = Arrays.asList("미등록-남자", "미등록-여자");

    Customer femaleCustomer = createUnregisteredCustomer(3L, "미등록-여자", Customer.Gender.F);

    given(
            customerJpaRepository.findByShopIdAndCustomerNameInAndDeletedAtIsNull(
                shopId, unregisteredNames))
        .willReturn(List.of(femaleCustomer));

    // when
    List<CustomerResponse> response = customerQueryService.getUnregisteredCustomers(shopId);

    // then
    assertThat(response).hasSize(1);
    assertThat(response.get(0).customerName()).isEqualTo("미등록-여자");

    then(customerJpaRepository)
        .should()
        .findByShopIdAndCustomerNameInAndDeletedAtIsNull(shopId, unregisteredNames);
  }

  @Test
  @DisplayName("미등록 고객 목록 조회 성공 - 존재하지 않음")
  void getUnregisteredCustomers_Success_NoneExist() {
    // given
    Long shopId = 1L;
    List<String> unregisteredNames = Arrays.asList("미등록-남자", "미등록-여자");

    given(
            customerJpaRepository.findByShopIdAndCustomerNameInAndDeletedAtIsNull(
                shopId, unregisteredNames))
        .willReturn(Collections.emptyList());

    // when
    List<CustomerResponse> response = customerQueryService.getUnregisteredCustomers(shopId);

    // then
    assertThat(response).isEmpty();

    then(customerJpaRepository)
        .should()
        .findByShopIdAndCustomerNameInAndDeletedAtIsNull(shopId, unregisteredNames);
  }

  @Test
  @DisplayName("세그먼트 태그별 고객 조회 성공")
  void getCustomersBySegmentTag_Success() {
    // given
    String segmentTag = "VIP";
    Long shopId = 1L;
    Long segmentId = 1L;
    String segmentTitle = "VIP 고객";

    var segment = createTestSegment(segmentId, segmentTag, segmentTitle);
    List<Long> allCustomerIds = List.of(1L, 2L, 3L);
    List<Customer> shopCustomers =
        List.of(createTestCustomerWithId(1L, shopId), createTestCustomerWithId(2L, shopId));

    given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.of(segment));
    given(segmentByCustomerRepository.findCustomerIdsBySegmentTag(segmentTag))
        .willReturn(allCustomerIds);
    given(customerJpaRepository.findAllById(allCustomerIds)).willReturn(shopCustomers);

    // when
    SegmentCustomersResponse response =
        customerQueryService.getCustomersBySegmentTag(segmentTag, shopId);

    // then
    assertThat(response.segmentTag()).isEqualTo(segmentTag);
    assertThat(response.segmentTitle()).isEqualTo(segmentTitle);
    assertThat(response.customerCount()).isEqualTo(2);
    assertThat(response.customerIds()).containsExactly(1L, 2L);

    then(segmentRepository).should().findBySegmentTag(segmentTag);
    then(segmentByCustomerRepository).should().findCustomerIdsBySegmentTag(segmentTag);
    then(customerJpaRepository).should().findAllById(allCustomerIds);
  }

  @Test
  @DisplayName("세그먼트 태그별 고객 조회 실패 - 세그먼트 없음")
  void getCustomersBySegmentTag_SegmentNotFound() {
    // given
    String segmentTag = "INVALID";
    Long shopId = 1L;

    given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentTag(segmentTag, shopId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("세그먼트를 찾을 수 없습니다");

    then(segmentRepository).should().findBySegmentTag(segmentTag);
  }

  @Test
  @DisplayName("세그먼트 ID별 고객 조회 성공")
  void getCustomersBySegmentId_Success() {
    // given
    Long segmentId = 1L;
    Long shopId = 1L;
    String segmentTag = "LOYAL";
    String segmentTitle = "충성 고객";

    var segment = createTestSegment(segmentId, segmentTag, segmentTitle);
    List<Long> allCustomerIds = List.of(1L, 2L, 3L);
    List<Customer> shopCustomers =
        List.of(createTestCustomerWithId(1L, shopId), createTestCustomerWithId(3L, shopId));

    given(segmentRepository.findById(segmentId)).willReturn(Optional.of(segment));
    given(segmentByCustomerRepository.findCustomerIdsBySegmentId(segmentId))
        .willReturn(allCustomerIds);
    given(customerJpaRepository.findAllById(allCustomerIds)).willReturn(shopCustomers);

    // when
    SegmentCustomersResponse response =
        customerQueryService.getCustomersBySegmentId(segmentId, shopId);

    // then
    assertThat(response.segmentTag()).isEqualTo(segmentTag);
    assertThat(response.segmentTitle()).isEqualTo(segmentTitle);
    assertThat(response.customerCount()).isEqualTo(2);
    assertThat(response.customerIds()).containsExactly(1L, 3L);

    then(segmentRepository).should().findById(segmentId);
    then(segmentByCustomerRepository).should().findCustomerIdsBySegmentId(segmentId);
    then(customerJpaRepository).should().findAllById(allCustomerIds);
  }

  @Test
  @DisplayName("다중 세그먼트별 고객 조회 성공")
  void getCustomersByMultipleSegmentTags_Success() {
    // given
    List<String> segmentTags = List.of("VIP", "LOYAL");
    Long shopId = 1L;

    var vipSegment = createTestSegment(1L, "VIP", "VIP 고객");
    var loyalSegment = createTestSegment(2L, "LOYAL", "충성 고객");
    List<com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment>
        segments = List.of(vipSegment, loyalSegment);

    List<Long> vipCustomerIds = List.of(1L, 2L);
    List<Long> loyalCustomerIds = List.of(3L, 4L);

    List<Customer> vipShopCustomers = List.of(createTestCustomerWithId(1L, shopId));
    List<Customer> loyalShopCustomers = List.of(createTestCustomerWithId(3L, shopId));

    given(segmentRepository.findBySegmentTagIn(segmentTags)).willReturn(segments);
    given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("VIP"))
        .willReturn(vipCustomerIds);
    given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("LOYAL"))
        .willReturn(loyalCustomerIds);
    given(customerJpaRepository.findAllById(vipCustomerIds)).willReturn(vipShopCustomers);
    given(customerJpaRepository.findAllById(loyalCustomerIds)).willReturn(loyalShopCustomers);

    // when
    List<SegmentCustomersResponse> responses =
        customerQueryService.getCustomersByMultipleSegmentTags(segmentTags, shopId);

    // then
    assertThat(responses).hasSize(2);

    SegmentCustomersResponse vipResponse = responses.get(0);
    assertThat(vipResponse.segmentTag()).isEqualTo("VIP");
    assertThat(vipResponse.segmentTitle()).isEqualTo("VIP 고객");
    assertThat(vipResponse.customerCount()).isEqualTo(1);
    assertThat(vipResponse.customerIds()).containsExactly(1L);

    SegmentCustomersResponse loyalResponse = responses.get(1);
    assertThat(loyalResponse.segmentTag()).isEqualTo("LOYAL");
    assertThat(loyalResponse.segmentTitle()).isEqualTo("충성 고객");
    assertThat(loyalResponse.customerCount()).isEqualTo(1);
    assertThat(loyalResponse.customerIds()).containsExactly(3L);

    then(segmentRepository).should().findBySegmentTagIn(segmentTags);
    then(segmentByCustomerRepository).should().findCustomerIdsBySegmentTag("VIP");
    then(segmentByCustomerRepository).should().findCustomerIdsBySegmentTag("LOYAL");
  }

  @Test
  @DisplayName("다중 세그먼트별 고객 조회 - 일부 세그먼트 없음")
  void getCustomersByMultipleSegmentTags_PartialSegmentNotFound() {
    // given
    List<String> segmentTags = List.of("VIP", "INVALID");
    Long shopId = 1L;

    var vipSegment = createTestSegment(1L, "VIP", "VIP 고객");
    List<com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment>
        segments = List.of(vipSegment); // INVALID 세그먼트는 없음

    List<Long> vipCustomerIds = List.of(1L);
    List<Customer> vipShopCustomers = List.of(createTestCustomerWithId(1L, shopId));

    given(segmentRepository.findBySegmentTagIn(segmentTags)).willReturn(segments);
    given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("VIP"))
        .willReturn(vipCustomerIds);
    given(customerJpaRepository.findAllById(vipCustomerIds)).willReturn(vipShopCustomers);

    // when
    List<SegmentCustomersResponse> responses =
        customerQueryService.getCustomersByMultipleSegmentTags(segmentTags, shopId);

    // then
    assertThat(responses).hasSize(2);

    SegmentCustomersResponse vipResponse = responses.get(0);
    assertThat(vipResponse.segmentTag()).isEqualTo("VIP");
    assertThat(vipResponse.customerCount()).isEqualTo(1);

    SegmentCustomersResponse invalidResponse = responses.get(1);
    assertThat(invalidResponse.segmentTag()).isEqualTo("INVALID");
    assertThat(invalidResponse.segmentTitle()).isEqualTo("Unknown Segment");
    assertThat(invalidResponse.customerCount()).isEqualTo(0);
    assertThat(invalidResponse.customerIds()).isEmpty();
  }

  // --- Helper Methods ---

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

  private Customer createUnregisteredCustomer(Long id, String name, Customer.Gender gender) {
    return Customer.builder()
        .id(id)
        .shopId(1L)
        .customerName(name)
        .gender(gender)
        .phoneNumber("01000000000")
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  private CustomerDetailResponse createTestCustomerDetailResponse() {
    return CustomerDetailResponse.builder()
        .customerId(1L)
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
        .marketingConsentedAt(null)
        .notificationConsent(false)
        .lastMessageSentAt(null)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .shopId(1L)
        .staff(CustomerDetailResponse.StaffInfo.builder().staffId(1L).staffName("김직원").build())
        .customerGrade(
            CustomerDetailResponse.CustomerGradeInfo.builder()
                .customerGradeId(1L)
                .customerGradeName("일반")
                .discountRate(0)
                .build())
        .acquisitionChannel(
            CustomerDetailResponse.AcquisitionChannelInfo.builder()
                .acquisitionChannelId(1L)
                .acquisitionChannelName("직접 방문")
                .build())
        .remainingPrepaidAmount(0)
        .build();
  }

  private CustomerListResponse createTestCustomerListResponse() {
    return new CustomerListResponse(
        1L, // customerId
        "홍길동", // customerName
        "01012345678", // phoneNumber
        "테스트 고객", // memo
        0, // visitCount
        0, // totalRevenue
        LocalDate.now(), // recentVisitDate
        LocalDate.of(1990, 1, 1), // birthdate
        "M", // gender
        1L, // customerGradeId
        "일반", // customerGradeName
        0, // discountRate
        1L, // staffId
        "김직원", // staffName
        1L, // acquisitionChannelId
        "직접 방문", // acquisitionChannelName
        0, // remainingPrepaidAmount
        0, // noshowCount
        LocalDateTime.now() // createdAt
        );
  }

  private CustomerDocument createTestCustomerDocument() {
    return CustomerDocument.builder()
        .id("1_1")
        .customerId(1L)
        .shopId(1L)
        .customerName("홍길동")
        .phoneNumber("01012345678")
        .customerGradeId(1L)
        .customerGradeName("일반")
        .gender("M")
        .deletedAt(null)
        .build();
  }

  private Customer createTestCustomerWithId(Long customerId, Long shopId) {
    return Customer.builder()
        .id(customerId)
        .customerGradeId(1L)
        .shopId(shopId)
        .staffId(1L)
        .customerName("테스트고객" + customerId)
        .phoneNumber("0101234567" + customerId)
        .memo("테스트 고객 " + customerId)
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

  private com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment
      createTestSegment(Long segmentId, String segmentTag, String segmentTitle) {
    return com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment
        .builder()
        .id(segmentId)
        .segmentTag(segmentTag)
        .segmentTitle(segmentTitle)
        .colorCode("#FF0000")
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }
}
