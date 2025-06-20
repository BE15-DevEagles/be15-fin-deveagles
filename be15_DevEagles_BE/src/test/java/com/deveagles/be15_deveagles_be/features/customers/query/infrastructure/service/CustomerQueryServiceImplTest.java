package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.service;

import static com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.QCustomer.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository.CustomerElasticsearchRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository.CustomerJpaRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDocument;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerListResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerSearchResult;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerDetailQueryRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.repository.CustomerListQueryRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  // QueryDSL 모킹 예제 - 블로그 참고:
  // https://stackoverflow.com/questions/50491750/%20junit-mockito-and-querydsl-mysema-for-mocking-jpaqueryfactory
  // StackOverflow 방식으로 시도했지만 복잡한 QueryDSL 체인(leftJoin, innerJoin 등)은 여전히 타입 문제로 모킹이 어려움
  // 간단한 select-from-where 패턴에만 적용 가능
  // 복잡한 join, projection(Tuple) 사용 쿼리는 통합 테스트(@DataJpaTest) 권장

  /*
  @Test
  @DisplayName("JPA 폴백 테스트 - StackOverflow 방식 시도")
  void testFallbackToJpa_StackOverflowApproach() {
    // given
    Long shopId = 1L;
    String searchKeyword = "홍길동";
    Pageable pageable = PageRequest.of(0, 10);

    // Mock 객체들
    JPAQueryFactory mockQueryFactory = mock(JPAQueryFactory.class);
    JPAQuery<Customer> mockJpaQuery = mock(JPAQuery.class);

    // 간단한 쿼리만 모킹 가능
    when(mockQueryFactory.selectFrom(any(EntityPath.class))).thenReturn(mockJpaQuery);
    when(mockJpaQuery.where(any(Predicate.class))).thenReturn(mockJpaQuery);
    when(mockJpaQuery.fetch()).thenReturn(Collections.emptyList());

    // 하지만 실제 서비스의 복잡한 쿼리는 여전히 모킹 불가:
    // queryFactory.select(customer.id, customer.customerName, customerGrade.customerGradeName)
    //   .from(customer)
    //   .leftJoin(customerGrade).on(customer.customerGradeId.eq(customerGrade.id))
    //   .where(conditions)
    //   .fetch()

    // 이런 복잡한 쿼리는 @DataJpaTest + 실제 DB 사용 권장
    assertThat(true).isTrue(); // 테스트 통과 확인용
  }

  @Test
  @DisplayName("동기화 테스트 - StackOverflow 방식으로도 한계")
  void testSyncCustomersToElasticsearch_MockingLimitation() {
    // QueryDSL의 복잡한 Projection과 Join은 단위 테스트에서 모킹하기 매우 어려움
    // 특히 Tuple 타입 반환하는 쿼리는 타입 안전성 문제로 모킹 불가

    // 실제 프로덕션에서는 다음과 같은 방법들 권장:
    // 1. @DataJpaTest + TestContainers로 실제 DB 테스트
    // 2. Repository 레이어와 Service 레이어 분리하여 Repository는 통합 테스트
    // 3. Service 레이어는 Repository 모킹으로 단위 테스트

    assertThat(true).isTrue(); // 테스트 통과 확인용
  }
  */

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

  // Elasticsearch 동기화 테스트는 QueryDSL 모킹이 복잡하여 통합 테스트에서 수행
  // @Test
  // @DisplayName("고객 Elasticsearch 동기화 성공")
  // void syncCustomerToElasticsearch_Success() {
  //   // QueryDSL JPAQueryFactory 모킹이 복잡하여 단위 테스트에서 제외
  // }

  // @Test
  // @DisplayName("매장별 전체 고객 재인덱싱 성공")
  // void reindexAllCustomers_Success() {
  //   // QueryDSL JPAQueryFactory 모킹이 복잡하여 단위 테스트에서 제외
  // }

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

  private Customer createTestCustomer() {
    return Customer.builder()
        .id(1L)
        .customerGradeId(1L)
        .shopId(1L)
        .staffId(1L)
        .customerName("홍길동")
        .phoneNumber("01012345678")
        .memo("테스트 고객")
        .visitCount(5)
        .totalRevenue(100000)
        .recentVisitDate(LocalDate.now())
        .birthdate(LocalDate.of(1990, 1, 1))
        .registeredAt(LocalDateTime.now())
        .noshowCount(0)
        .gender(Customer.Gender.M)
        .marketingConsent(true)
        .notificationConsent(true)
        .channelId(1L)
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
        .visitCount(5)
        .totalRevenue(100000)
        .recentVisitDate(LocalDate.now())
        .birthdate(LocalDate.of(1990, 1, 1))
        .registeredAt(LocalDateTime.now())
        .noshowCount(0)
        .gender(Customer.Gender.M)
        .marketingConsent(true)
        .notificationConsent(true)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .shopId(1L)
        .staffId(1L)
        .customerGradeName("VIP")
        .discountRate(10)
        .channelName("네이버 블로그")
        .tags(List.of())
        .build();
  }

  private CustomerListResponse createTestCustomerListResponse() {
    return CustomerListResponse.builder()
        .customerId(1L)
        .customerName("홍길동")
        .phoneNumber("01012345678")
        .memo("테스트 고객")
        .visitCount(5)
        .totalRevenue(100000)
        .recentVisitDate(LocalDate.now())
        .birthdate(LocalDate.of(1990, 1, 1))
        .gender("M")
        .customerGradeName("VIP")
        .discountRate(10)
        .staffId(1L)
        .remainingPrepaidAmount(0)
        .tags(List.of())
        .build();
  }

  private CustomerDocument createTestCustomerDocument() {
    return CustomerDocument.builder()
        .id("1_1")
        .customerId(1L)
        .shopId(1L)
        .customerName("홍길동")
        .phoneNumber("01012345678")
        .customerGradeId(1L)
        .customerGradeName("VIP")
        .gender("M")
        .deletedAt(null)
        .build();
  }
}
