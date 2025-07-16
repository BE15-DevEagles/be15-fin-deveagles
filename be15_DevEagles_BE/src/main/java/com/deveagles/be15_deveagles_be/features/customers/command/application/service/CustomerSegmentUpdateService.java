package com.deveagles.be15_deveagles_be.features.customers.command.application.service;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.SegmentByCustomer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSegmentUpdateService {

  private final CustomerRepository customerRepository;
  private final SegmentRepository segmentRepository;
  private final SegmentByCustomerRepository segmentByCustomerRepository;

  private static final String NEW_SEGMENT = "NEW";
  private static final String GROWING_SEGMENT = "GROWING";
  private static final String LOYAL_SEGMENT = "LOYAL";
  private static final String VIP_SEGMENT = "VIP";
  private static final String DORMANT_SEGMENT = "DORMANT";

  // 신규 세그먼트 추가
  private static final String NEW_FOLLOWUP_SEGMENT = "NEW_FOLLOWUP";
  private static final String NEW_AT_RISK_SEGMENT = "NEW_AT_RISK";
  private static final String REACTIVATION_NEEDED_SEGMENT = "REACTIVATION_NEEDED";
  private static final String GROWING_DELAYED_SEGMENT = "GROWING_DELAYED";
  private static final String LOYAL_DELAYED_SEGMENT = "LOYAL_DELAYED";

  private static final int BATCH_SIZE = 1000;

  @Transactional
  public void updateAllCustomerSegments() {
    log.info("고객 세그먼트 업데이트 시작 (최적화된 일괄삭제 방식)");

    // 1. 생명주기 세그먼트 정보 로드
    Map<String, Segment> lifecycleSegmentsMap =
        segmentRepository.findLifecycleSegments().stream()
            .collect(Collectors.toMap(s -> s.getSegmentTag().toUpperCase(), s -> s));

    // 2. 기존 라이프사이클 세그먼트 일괄 삭제 (이탈위험 세그먼트는 보호됨)
    log.info("기존 라이프사이클 세그먼트 일괄 삭제 시작");
    segmentByCustomerRepository.deleteAllLifecycleSegments();
    log.info("기존 라이프사이클 세그먼트 일괄 삭제 완료");

    // 3. 전체 고객 수 조회
    long totalCustomers = customerRepository.count();
    log.info("처리할 총 고객 수: {}", totalCustomers);

    // 4. 청크 단위로 고객 처리하여 새 세그먼트 할당
    List<SegmentByCustomer> newAssignments = new ArrayList<>();
    int processedCustomers = 0;
    int page = 0;

    while (processedCustomers < totalCustomers) {
      // 페이지 단위로 고객 조회
      List<Customer> customerChunk = customerRepository.findAllWithPagination(page, BATCH_SIZE);

      if (customerChunk.isEmpty()) {
        break;
      }

      // 청크 내 고객들의 새 세그먼트 계산 및 할당 준비
      for (Customer customer : customerChunk) {
        String newSegmentTag = determineCustomerSegment(customer);
        Segment newSegment = lifecycleSegmentsMap.get(newSegmentTag);

        if (newSegment == null) {
          log.warn("정의되지 않은 세그먼트 태그: {}", newSegmentTag);
          continue;
        }

        // 새 세그먼트 할당 목록에 추가
        newAssignments.add(
            SegmentByCustomer.builder()
                .customerId(customer.getId())
                .segmentId(newSegment.getId())
                .build());
      }

      processedCustomers += customerChunk.size();
      page++;

      // 배치 크기만큼 쌓이면 DB 일괄 삽입 수행
      if (newAssignments.size() >= BATCH_SIZE) {
        segmentByCustomerRepository.saveAll(newAssignments);
        log.info("{}개의 신규 세그먼트 할당 추가", newAssignments.size());
        newAssignments.clear();
      }

      log.info("진행 상황: {}/{} 고객 처리 완료", processedCustomers, totalCustomers);
    }

    // 5. 남은 배치 처리
    if (!newAssignments.isEmpty()) {
      segmentByCustomerRepository.saveAll(newAssignments);
      log.info("{}개의 신규 세그먼트 할당 추가 (최종)", newAssignments.size());
    }

    log.info("고객 세그먼트 업데이트 완료. 처리된 고객 수: {}", processedCustomers);
  }

  private String determineCustomerSegment(Customer customer) {
    // 1순위: 휴면 고객 (6개월 이상 미방문)
    if (isDormantCustomer(customer)) {
      return DORMANT_SEGMENT;
    }

    // 2순위: 신규 고객 재활성화 필요 (신규 고객 90일 이상 미방문)
    if (isReactivationNeeded(customer)) {
      return REACTIVATION_NEEDED_SEGMENT;
    }

    // 3순위: 신규 고객 이탈 위험 (신규 고객 60일 이상 미방문)
    if (isNewAtRisk(customer)) {
      return NEW_AT_RISK_SEGMENT;
    }

    // 4순위: 충성 고객 방문 지연 (충성 고객이면서 90일 미방문)
    if (isLoyalDelayed(customer)) {
      return LOYAL_DELAYED_SEGMENT;
    }

    // 5순위: 성장 고객 방문 지연 (성장 고객이면서 90일 미방문)
    if (isGrowingDelayed(customer)) {
      return GROWING_DELAYED_SEGMENT;
    }

    // 6순위: 신규 고객 팔로업 필요 (가입 후 30일 이후)
    if (isNewFollowupNeeded(customer)) {
      return NEW_FOLLOWUP_SEGMENT;
    }

    // 7순위: VIP 고객 (고액 결제 + 정기 방문)
    if (isVipCustomer(customer)) {
      return VIP_SEGMENT;
    }

    // 8순위: 충성 고객 (방문 10회 이상 + 최근 활동)
    if (isLoyalCustomer(customer)) {
      return LOYAL_SEGMENT;
    }

    // 9순위: 성장 고객 (방문 3~9회 + 최근 활동)
    if (isGrowingCustomer(customer)) {
      return GROWING_SEGMENT;
    }

    // 10순위: 신규 고객 (첫 방문 후 30일 이내, 방문 3회 미만)
    if (isNewCustomer(customer)) {
      return NEW_SEGMENT;
    }

    // 기본값: 신규 고객
    return NEW_SEGMENT;
  }

  private boolean isDormantCustomer(Customer customer) {
    if (customer.getRecentVisitDate() == null) {
      return false;
    }
    LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
    return customer.getRecentVisitDate().isBefore(sixMonthsAgo);
  }

  private boolean isNewCustomer(Customer customer) {
    if (customer.getCreatedAt() == null || customer.getVisitCount() == null) {
      return false;
    }

    LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
    LocalDate customerCreatedDate = customer.getCreatedAt().toLocalDate();
    boolean isWithinThirtyDays = customerCreatedDate.isAfter(thirtyDaysAgo);
    boolean hasLessThanThreeVisits = customer.getVisitCount() < 3;

    return isWithinThirtyDays && hasLessThanThreeVisits;
  }

  private boolean isVipCustomer(Customer customer) {
    if (customer.getTotalRevenue() == null || customer.getVisitCount() == null) {
      return false;
    }

    boolean hasHighRevenue = customer.getTotalRevenue() >= 1000000;
    boolean hasRegularVisits = customer.getVisitCount() >= 10;

    return hasHighRevenue && hasRegularVisits;
  }

  private boolean isLoyalCustomer(Customer customer) {
    if (customer.getVisitCount() == null) {
      return false;
    }

    boolean hasFrequentVisits = customer.getVisitCount() >= 10;
    boolean hasRecentActivity = isRecentlyActive(customer);

    return hasFrequentVisits && hasRecentActivity;
  }

  private boolean isGrowingCustomer(Customer customer) {
    if (customer.getVisitCount() == null) {
      return false;
    }

    boolean hasModerateVisits = customer.getVisitCount() >= 3 && customer.getVisitCount() < 10;
    boolean hasRecentActivity = isRecentlyActive(customer);

    return hasModerateVisits && hasRecentActivity;
  }

  private boolean isRecentlyActive(Customer customer) {
    if (customer.getRecentVisitDate() == null) {
      return false;
    }
    LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
    return customer.getRecentVisitDate().isAfter(threeMonthsAgo);
  }

  private boolean isNewFollowupNeeded(Customer customer) {
    if (customer.getCreatedAt() == null || customer.getVisitCount() == null) {
      return false;
    }

    LocalDate customerCreatedDate = customer.getCreatedAt().toLocalDate();
    LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
    LocalDate sixtyDaysAgo = LocalDate.now().minusDays(60);

    // 가입 후 30일 이후이면서 60일 이전, 방문 3회 미만
    boolean isAfterThirtyDays = customerCreatedDate.isBefore(thirtyDaysAgo);
    boolean isBeforeSixtyDays = customerCreatedDate.isAfter(sixtyDaysAgo);
    boolean hasLessThanThreeVisits = customer.getVisitCount() < 3;

    return isAfterThirtyDays && isBeforeSixtyDays && hasLessThanThreeVisits;
  }

  private boolean isNewAtRisk(Customer customer) {
    if (customer.getCreatedAt() == null
        || customer.getRecentVisitDate() == null
        || customer.getVisitCount() == null) {
      return false;
    }

    LocalDate customerCreatedDate = customer.getCreatedAt().toLocalDate();
    LocalDate sixtyDaysAgo = LocalDate.now().minusDays(60);
    LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
    LocalDate sixtyDaysAgoFromToday = LocalDate.now().minusDays(60);

    // 신규 고객이면서 60일 이상 미방문
    boolean isNewCustomerByAge = customerCreatedDate.isAfter(ninetyDaysAgo); // 신규 고객 (90일 이내 가입)
    boolean hasNotVisitedFor60Days = customer.getRecentVisitDate().isBefore(sixtyDaysAgoFromToday);
    boolean hasLessThanThreeVisits = customer.getVisitCount() < 3;

    return isNewCustomerByAge && hasNotVisitedFor60Days && hasLessThanThreeVisits;
  }

  private boolean isReactivationNeeded(Customer customer) {
    if (customer.getCreatedAt() == null
        || customer.getRecentVisitDate() == null
        || customer.getVisitCount() == null) {
      return false;
    }

    LocalDate customerCreatedDate = customer.getCreatedAt().toLocalDate();
    LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
    LocalDate ninetyDaysAgoFromToday = LocalDate.now().minusDays(90);

    // 신규 고객이면서 90일 이상 미방문
    boolean isNewCustomerByAge = customerCreatedDate.isAfter(ninetyDaysAgo); // 신규 고객 (90일 이내 가입)
    boolean hasNotVisitedFor90Days = customer.getRecentVisitDate().isBefore(ninetyDaysAgoFromToday);
    boolean hasLessThanThreeVisits = customer.getVisitCount() < 3;

    return isNewCustomerByAge && hasNotVisitedFor90Days && hasLessThanThreeVisits;
  }

  private boolean isGrowingDelayed(Customer customer) {
    if (customer.getVisitCount() == null || customer.getRecentVisitDate() == null) {
      return false;
    }

    // 성장 고객 조건: 방문 3~9회
    boolean isGrowingCustomerByVisits =
        customer.getVisitCount() >= 3 && customer.getVisitCount() < 10;

    // 90일 이상 미방문
    LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
    boolean hasNotVisitedFor90Days = customer.getRecentVisitDate().isBefore(ninetyDaysAgo);

    return isGrowingCustomerByVisits && hasNotVisitedFor90Days;
  }

  private boolean isLoyalDelayed(Customer customer) {
    if (customer.getVisitCount() == null || customer.getRecentVisitDate() == null) {
      return false;
    }

    // 충성 고객 조건: 방문 10회 이상
    boolean isLoyalCustomerByVisits = customer.getVisitCount() >= 10;

    // 90일 이상 미방문
    LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
    boolean hasNotVisitedFor90Days = customer.getRecentVisitDate().isBefore(ninetyDaysAgo);

    return isLoyalCustomerByVisits && hasNotVisitedFor90Days;
  }
}
