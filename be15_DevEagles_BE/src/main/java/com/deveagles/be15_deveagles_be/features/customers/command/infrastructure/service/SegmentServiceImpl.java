package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.SegmentService;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.SegmentByCustomer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SegmentServiceImpl implements SegmentService {

  private final SegmentRepository segmentRepository;
  private final SegmentByCustomerRepository segmentByCustomerRepository;

  @Override
  public void assignSegmentToCustomer(Long customerId, String segmentTag) {
    log.info("고객 세그먼트 할당: customerId={}, segmentTag={}", customerId, segmentTag);

    Optional<Segment> segmentOpt = segmentRepository.findBySegmentTag(segmentTag);
    if (segmentOpt.isEmpty()) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "세그먼트를 찾을 수 없습니다: " + segmentTag);
    }

    Segment segment = segmentOpt.get();

    // 중복 체크
    if (segmentByCustomerRepository.existsByCustomerIdAndSegmentId(customerId, segment.getId())) {
      log.debug("이미 할당된 세그먼트: customerId={}, segmentTag={}", customerId, segmentTag);
      return;
    }

    // 세그먼트 할당
    SegmentByCustomer segmentByCustomer =
        SegmentByCustomer.builder().customerId(customerId).segmentId(segment.getId()).build();

    segmentByCustomerRepository.save(segmentByCustomer);
    log.info("고객 세그먼트 할당 완료: customerId={}, segmentTag={}", customerId, segmentTag);
  }

  @Override
  public void assignSegmentsToCustomer(Long customerId, List<String> segmentTags) {
    log.info("고객 다중 세그먼트 할당: customerId={}, segmentTags={}", customerId, segmentTags);

    for (String segmentTag : segmentTags) {
      try {
        assignSegmentToCustomer(customerId, segmentTag);
      } catch (Exception e) {
        log.error(
            "세그먼트 할당 실패: customerId={}, segmentTag={}, error={}",
            customerId,
            segmentTag,
            e.getMessage());
      }
    }
  }

  @Override
  public void removeSegmentFromCustomer(Long customerId, String segmentTag) {
    log.info("고객 세그먼트 제거: customerId={}, segmentTag={}", customerId, segmentTag);

    Optional<Segment> segmentOpt = segmentRepository.findBySegmentTag(segmentTag);
    if (segmentOpt.isEmpty()) {
      log.warn("세그먼트를 찾을 수 없음: segmentTag={}", segmentTag);
      return;
    }

    Segment segment = segmentOpt.get();

    if (!segmentByCustomerRepository.existsByCustomerIdAndSegmentId(customerId, segment.getId())) {
      log.debug("할당되지 않은 세그먼트: customerId={}, segmentTag={}", customerId, segmentTag);
      return;
    }

    segmentByCustomerRepository.deleteByCustomerIdAndSegmentId(customerId, segment.getId());
    log.info("고객 세그먼트 제거 완료: customerId={}, segmentTag={}", customerId, segmentTag);
  }

  @Override
  public void removeAllSegmentsFromCustomer(Long customerId) {
    log.info("고객 전체 세그먼트 제거: customerId={}", customerId);

    int deletedCount = segmentByCustomerRepository.findByCustomerId(customerId).size();
    segmentByCustomerRepository.deleteByCustomerId(customerId);

    log.info("고객 전체 세그먼트 제거 완료: customerId={}, count={}", customerId, deletedCount);
  }

  @Override
  public void removeRiskSegmentsFromCustomer(Long customerId) {
    log.info("고객 위험 세그먼트 제거: customerId={}", customerId);

    List<SegmentByCustomer> riskSegments =
        segmentByCustomerRepository.findRiskSegmentsByCustomerId(customerId, "risk");

    for (SegmentByCustomer segmentByCustomer : riskSegments) {
      segmentByCustomerRepository.deleteByCustomerIdAndSegmentId(
          customerId, segmentByCustomer.getSegmentId());
    }

    log.info("고객 위험 세그먼트 제거 완료: customerId={}, count={}", customerId, riskSegments.size());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Segment> getCustomerSegments(Long customerId) {
    log.debug("고객 세그먼트 조회: customerId={}", customerId);

    List<SegmentByCustomer> segmentByCustomers =
        segmentByCustomerRepository.findByCustomerId(customerId);

    return segmentByCustomers.stream()
        .map(sbc -> segmentRepository.findById(sbc.getSegmentId()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getCustomerIdsBySegmentTag(String segmentTag) {
    log.debug("세그먼트별 고객 ID 조회: segmentTag={}", segmentTag);

    return segmentByCustomerRepository.findCustomerIdsBySegmentTag(segmentTag);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getCustomerIdsBySegmentTags(List<String> segmentTags) {
    log.debug("다중 세그먼트별 고객 ID 조회: segmentTags={}", segmentTags);

    return segmentByCustomerRepository.findCustomerIdsBySegmentTags(segmentTags);
  }

  @Override
  public Segment createSegmentIfNotExists(
      String segmentTag, String segmentTitle, String colorCode) {
    log.info("세그먼트 생성 (존재하지 않는 경우): segmentTag={}, segmentTitle={}", segmentTag, segmentTitle);

    Optional<Segment> existingSegment = segmentRepository.findBySegmentTag(segmentTag);
    if (existingSegment.isPresent()) {
      log.debug("이미 존재하는 세그먼트: segmentTag={}", segmentTag);
      return existingSegment.get();
    }

    Segment newSegment =
        Segment.builder()
            .segmentTag(segmentTag)
            .segmentTitle(segmentTitle)
            .colorCode(colorCode)
            .build();

    Segment savedSegment = segmentRepository.save(newSegment);
    log.info("새 세그먼트 생성 완료: segmentTag={}, id={}", segmentTag, savedSegment.getId());

    return savedSegment;
  }
}
