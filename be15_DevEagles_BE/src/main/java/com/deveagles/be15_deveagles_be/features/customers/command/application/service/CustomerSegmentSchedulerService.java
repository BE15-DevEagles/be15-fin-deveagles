package com.deveagles.be15_deveagles_be.features.customers.command.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSegmentSchedulerService {

  private final CustomerSegmentUpdateService customerSegmentUpdateService;

  @Scheduled(cron = "0 0 3 * * *")
  public void updateCustomerSegmentsDaily() {
    log.info("매일 새벽 3시 고객 세그먼트 업데이트 스케줄러 실행");

    try {
      customerSegmentUpdateService.updateAllCustomerSegments();
      log.info("고객 세그먼트 업데이트 스케줄러 실행 완료");
    } catch (Exception e) {
      log.error("고객 세그먼트 업데이트 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
    }
  }
}
