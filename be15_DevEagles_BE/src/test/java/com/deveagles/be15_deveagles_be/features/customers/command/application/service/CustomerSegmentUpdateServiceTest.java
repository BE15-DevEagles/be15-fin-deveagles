package com.deveagles.be15_deveagles_be.features.customers.command.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("고객 세그먼트 업데이트 서비스 테스트")
class CustomerSegmentUpdateServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentByCustomerRepository segmentByCustomerRepository;

  @InjectMocks private CustomerSegmentUpdateService customerSegmentUpdateService;

  private Segment newSegment;
  private Segment growingSegment;
  private Segment loyalSegment;
  private Segment vipSegment;
  private Segment dormantSegment;
  private Segment newFollowupSegment;
  private Segment newAtRiskSegment;
  private Segment reactivationNeededSegment;
  private Segment growingDelayedSegment;
  private Segment loyalDelayedSegment;

  @BeforeEach
  void setUp() {
    // 세그먼트 객체 초기화
    newSegment = createSegment(1L, "NEW", "신규 고객");
    growingSegment = createSegment(2L, "GROWING", "성장 고객");
    loyalSegment = createSegment(3L, "LOYAL", "충성 고객");
    vipSegment = createSegment(4L, "VIP", "VIP 고객");
    dormantSegment = createSegment(5L, "DORMANT", "휴면 고객");
    newFollowupSegment = createSegment(6L, "NEW_FOLLOWUP", "신규 고객 팔로업 필요");
    newAtRiskSegment = createSegment(7L, "NEW_AT_RISK", "신규 고객 이탈 위험");
    reactivationNeededSegment = createSegment(8L, "REACTIVATION_NEEDED", "재활성화 필요");
    growingDelayedSegment = createSegment(9L, "GROWING_DELAYED", "성장 고객 방문 지연");
    loyalDelayedSegment = createSegment(10L, "LOYAL_DELAYED", "충성 고객 방문 지연");
  }

  private Segment createSegment(Long id, String tag, String title) {
    return Segment.builder()
        .id(id)
        .segmentTag(tag)
        .segmentTitle(title)
        .colorCode("#00BFFF")
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  private Customer createCustomer(
      Long id,
      LocalDateTime createdAt,
      LocalDate recentVisitDate,
      Integer visitCount,
      Integer totalRevenue) {
    return Customer.builder()
        .id(id)
        .customerGradeId(1L)
        .shopId(1L)
        .staffId(1L)
        .customerName("고객" + id)
        .phoneNumber("010000000" + id)
        .visitCount(visitCount)
        .totalRevenue(totalRevenue)
        .recentVisitDate(recentVisitDate)
        .birthdate(LocalDate.of(1990, 1, 1))
        .createdAt(createdAt)
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("고객 세그먼트 전체 업데이트가 성공적으로 실행된다")
  void updateAllCustomerSegments_Success() {
    // given
    List<Segment> lifecycleSegments =
        Arrays.asList(
            newSegment,
            growingSegment,
            loyalSegment,
            vipSegment,
            dormantSegment,
            newFollowupSegment,
            newAtRiskSegment,
            reactivationNeededSegment,
            growingDelayedSegment,
            loyalDelayedSegment);

    Customer customer1 =
        createCustomer(
            1L, LocalDateTime.now().minusDays(10), LocalDate.now().minusDays(1), 1, 50000);
    Customer customer2 =
        createCustomer(
            2L, LocalDateTime.now().minusDays(50), LocalDate.now().minusDays(2), 5, 200000);

    List<Customer> customers = Arrays.asList(customer1, customer2);

    given(segmentRepository.findLifecycleSegments()).willReturn(lifecycleSegments);
    given(customerRepository.count()).willReturn(2L);
    given(customerRepository.findAllWithPagination(anyInt(), anyInt()))
        .willReturn(customers)
        .willReturn(Collections.emptyList());

    doNothing().when(segmentByCustomerRepository).deleteAllLifecycleSegments();

    // when
    customerSegmentUpdateService.updateAllCustomerSegments();

    // then
    then(segmentRepository).should().findLifecycleSegments();
    then(segmentByCustomerRepository).should().deleteAllLifecycleSegments();
    then(customerRepository).should().count();
    then(customerRepository).should(times(1)).findAllWithPagination(anyInt(), anyInt());
    then(segmentByCustomerRepository).should().saveAll(anyList());
  }

  @Nested
  @DisplayName("세그먼트 분류 로직 테스트")
  class SegmentClassificationTest {

    @Test
    @DisplayName("휴면 고객으로 분류된다 - 6개월 이상 미방문")
    void determineDormantCustomer() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(8), LocalDate.now().minusMonths(7), 10, 500000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("DORMANT");
    }

    @Test
    @DisplayName("재활성화 필요 고객으로 분류된다 - 신규 고객 90일 이상 미방문")
    void determineReactivationNeeded() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(60), LocalDate.now().minusDays(95), 2, 50000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("REACTIVATION_NEEDED");
    }

    @Test
    @DisplayName("신규 이탈 위험 고객으로 분류된다 - 신규 고객 60일 이상 미방문")
    void determineNewAtRisk() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(70), LocalDate.now().minusDays(65), 2, 50000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("NEW_AT_RISK");
    }

    @Test
    @DisplayName("충성 고객 방문 지연으로 분류된다 - 방문 10회 이상, 90일 미방문")
    void determineLoyalDelayed() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(95), 15, 800000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("LOYAL_DELAYED");
    }

    @Test
    @DisplayName("성장 고객 방문 지연으로 분류된다 - 방문 3~9회, 90일 미방문")
    void determineGrowingDelayed() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(4), LocalDate.now().minusDays(95), 5, 300000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("GROWING_DELAYED");
    }

    @Test
    @DisplayName("신규 고객 팔로업 필요로 분류된다 - 가입 후 30일 이후, 60일 이전")
    void determineNewFollowup() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(45), LocalDate.now().minusDays(10), 2, 100000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("NEW_FOLLOWUP");
    }

    @Test
    @DisplayName("VIP 고객으로 분류된다 - 고액 결제 + 정기 방문")
    void determineVipCustomer() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(5), 15, 1200000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("VIP");
    }

    @Test
    @DisplayName("충성 고객으로 분류된다 - 방문 10회 이상 + 최근 활동")
    void determineLoyalCustomer() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(30), 12, 600000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("LOYAL");
    }

    @Test
    @DisplayName("성장 고객으로 분류된다 - 방문 3~9회 + 최근 활동")
    void determineGrowingCustomer() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(3), LocalDate.now().minusDays(30), 5, 300000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("GROWING");
    }

    @Test
    @DisplayName("신규 고객으로 분류된다 - 첫 방문 후 30일 이내, 방문 3회 미만")
    void determineNewCustomer() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(20), LocalDate.now().minusDays(5), 2, 100000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("NEW");
    }

    @Test
    @DisplayName("기본값으로 신규 고객이 반환된다")
    void determineDefaultSegment() throws Exception {
      // given
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(100), LocalDate.now().minusDays(10), 1, 20000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("NEW");
    }

    private String callDetermineCustomerSegment(Customer customer) throws Exception {
      Method method =
          CustomerSegmentUpdateService.class.getDeclaredMethod(
              "determineCustomerSegment", Customer.class);
      method.setAccessible(true);
      return (String) method.invoke(customerSegmentUpdateService, customer);
    }
  }

  @Nested
  @DisplayName("개별 세그먼트 조건 테스트")
  class IndividualSegmentConditionTest {

    @Test
    @DisplayName("휴면 고객 조건 확인")
    void isDormantCustomer() throws Exception {
      // given - 6개월 이상 미방문
      Customer dormantCustomer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(8), LocalDate.now().minusMonths(7), 10, 500000);
      Customer activeCustomer =
          createCustomer(
              2L, LocalDateTime.now().minusMonths(8), LocalDate.now().minusDays(30), 10, 500000);

      // when
      boolean isDormant1 = callBooleanMethod("isDormantCustomer", dormantCustomer);
      boolean isDormant2 = callBooleanMethod("isDormantCustomer", activeCustomer);

      // then
      assertThat(isDormant1).isTrue();
      assertThat(isDormant2).isFalse();
    }

    @Test
    @DisplayName("신규 고객 조건 확인")
    void isNewCustomer() throws Exception {
      // given
      Customer newCustomer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(20), LocalDate.now().minusDays(5), 2, 100000);
      Customer oldCustomer =
          createCustomer(
              2L, LocalDateTime.now().minusDays(40), LocalDate.now().minusDays(5), 2, 100000);
      Customer frequentCustomer =
          createCustomer(
              3L, LocalDateTime.now().minusDays(20), LocalDate.now().minusDays(5), 5, 100000);

      // when
      boolean isNew1 = callBooleanMethod("isNewCustomer", newCustomer);
      boolean isNew2 = callBooleanMethod("isNewCustomer", oldCustomer);
      boolean isNew3 = callBooleanMethod("isNewCustomer", frequentCustomer);

      // then
      assertThat(isNew1).isTrue();
      assertThat(isNew2).isFalse();
      assertThat(isNew3).isFalse();
    }

    @Test
    @DisplayName("VIP 고객 조건 확인")
    void isVipCustomer() throws Exception {
      // given
      Customer vipCustomer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(5), 15, 1200000);
      Customer lowRevenueCustomer =
          createCustomer(
              2L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(5), 15, 500000);
      Customer lowVisitCustomer =
          createCustomer(
              3L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(5), 5, 1200000);

      // when
      boolean isVip1 = callBooleanMethod("isVipCustomer", vipCustomer);
      boolean isVip2 = callBooleanMethod("isVipCustomer", lowRevenueCustomer);
      boolean isVip3 = callBooleanMethod("isVipCustomer", lowVisitCustomer);

      // then
      assertThat(isVip1).isTrue();
      assertThat(isVip2).isFalse();
      assertThat(isVip3).isFalse();
    }

    @Test
    @DisplayName("신규 팔로업 필요 조건 확인")
    void isNewFollowupNeeded() throws Exception {
      // given
      Customer followupCustomer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(45), LocalDate.now().minusDays(10), 2, 100000);
      Customer tooEarlyCustomer =
          createCustomer(
              2L, LocalDateTime.now().minusDays(20), LocalDate.now().minusDays(10), 2, 100000);
      Customer tooLateCustomer =
          createCustomer(
              3L, LocalDateTime.now().minusDays(70), LocalDate.now().minusDays(10), 2, 100000);

      // when
      boolean needsFollowup1 = callBooleanMethod("isNewFollowupNeeded", followupCustomer);
      boolean needsFollowup2 = callBooleanMethod("isNewFollowupNeeded", tooEarlyCustomer);
      boolean needsFollowup3 = callBooleanMethod("isNewFollowupNeeded", tooLateCustomer);

      // then
      assertThat(needsFollowup1).isTrue();
      assertThat(needsFollowup2).isFalse();
      assertThat(needsFollowup3).isFalse();
    }

    private boolean callBooleanMethod(String methodName, Customer customer) throws Exception {
      Method method =
          CustomerSegmentUpdateService.class.getDeclaredMethod(methodName, Customer.class);
      method.setAccessible(true);
      return (Boolean) method.invoke(customerSegmentUpdateService, customer);
    }
  }

  @Nested
  @DisplayName("우선순위 테스트")
  class PriorityTest {

    @Test
    @DisplayName("휴면 고객이 최우선으로 분류된다")
    void dormantCustomerHasHighestPriority() throws Exception {
      // given - 휴면 고객이면서 VIP 조건도 만족하는 고객
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(8), LocalDate.now().minusMonths(7), 15, 1500000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("DORMANT");
    }

    @Test
    @DisplayName("재활성화 필요가 신규 이탈 위험보다 우선한다")
    void reactivationNeededHasPriorityOverNewAtRisk() throws Exception {
      // given - 신규 고객 90일 이상 미방문 (재활성화 필요 조건)
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusDays(60), LocalDate.now().minusDays(95), 2, 50000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("REACTIVATION_NEEDED");
    }

    @Test
    @DisplayName("지연 세그먼트가 일반 세그먼트보다 우선한다")
    void delayedSegmentsHavePriorityOverRegular() throws Exception {
      // given - 충성 고객이지만 90일 이상 미방문
      Customer customer =
          createCustomer(
              1L, LocalDateTime.now().minusMonths(6), LocalDate.now().minusDays(95), 15, 800000);

      // when
      String result = callDetermineCustomerSegment(customer);

      // then
      assertThat(result).isEqualTo("LOYAL_DELAYED");
    }

    private String callDetermineCustomerSegment(Customer customer) throws Exception {
      Method method =
          CustomerSegmentUpdateService.class.getDeclaredMethod(
              "determineCustomerSegment", Customer.class);
      method.setAccessible(true);
      return (String) method.invoke(customerSegmentUpdateService, customer);
    }
  }
}
