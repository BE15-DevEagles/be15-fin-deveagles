package com.deveagles.be15_deveagles_be.features.workflows.execution.infrastructure.service;

import com.deveagles.be15_deveagles_be.features.coupons.application.query.CouponQueryService;
import com.deveagles.be15_deveagles_be.features.coupons.presentation.dto.response.CouponResponse;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceAdapter {

  private final CouponQueryService couponQueryService;

  public boolean isValidCoupon(String couponCode, Long shopId) {
    try {
      if (couponCode == null || couponCode.trim().isEmpty()) {
        log.warn("쿠폰 코드가 비어 있습니다");
        return false;
      }

      Optional<CouponResponse> optCoupon = couponQueryService.getCouponByCode(couponCode, shopId);

      if (optCoupon.isEmpty()) {
        log.warn("쿠폰을 찾을 수 없습니다. couponCode: {}", couponCode);
        return false;
      }

      CouponResponse coupon = optCoupon.get();

      // 매장 사용 가능 여부
      if (!shopId.equals(coupon.getShopId())) {
        log.warn(
            "해당 매장에서 사용할 수 없는 쿠폰입니다. couponCode: {}, shopId: {}, coupon.shopId: {}",
            couponCode,
            shopId,
            coupon.getShopId());
        return false;
      }

      // 활성화 여부
      if (Boolean.FALSE.equals(coupon.getIsActive())) {
        log.warn("비활성화된 쿠폰입니다. couponCode: {}", couponCode);
        return false;
      }

      if (coupon.getExpirationDate() != null
          && coupon.getExpirationDate().isBefore(LocalDate.now())) {
        log.warn("만료일이 지난 쿠폰입니다. couponCode: {}", couponCode);
        return false;
      }

      log.info("쿠폰이 유효합니다. couponCode: {}, shopId: {}", couponCode, shopId);
      return true;

    } catch (Exception e) {
      log.error("쿠폰 검증 중 예외 발생. couponCode: {}, shopId: {}", couponCode, shopId, e);
      return false;
    }
  }

  /** 쿠폰 ID로 실제 쿠폰 코드를 조회합니다. 워크플로우에서 쿠폰 ID를 저장하지만, 메시지 발송 시에는 실제 쿠폰 코드가 필요하기 때문입니다. */
  public String getCouponCodeById(String couponId, Long shopId) {
    try {
      if (couponId == null || couponId.trim().isEmpty()) {
        log.warn("쿠폰 ID가 비어 있습니다");
        return null;
      }

      // TODO: CouponQueryService에 ID로 조회하는 메서드가 필요할 수 있습니다.
      // 현재는 임시로 couponId를 그대로 반환합니다.
      // 실제 구현에서는 쿠폰 ID로 쿠폰을 조회한 후 쿠폰 코드를 반환해야 합니다.

      log.info("쿠폰 ID로 코드 조회: couponId={}, shopId={}", couponId, shopId);

      // 임시 처리: ID가 숫자인 경우 "COUPON" + ID 형태로 변환
      // 실제로는 데이터베이스에서 조회해야 함
      try {
        Long.parseLong(couponId);
        return "COUPON" + couponId;
      } catch (NumberFormatException e) {
        // 이미 쿠폰 코드 형태인 경우 그대로 반환
        return couponId;
      }

    } catch (Exception e) {
      log.error("쿠폰 코드 조회 중 예외 발생. couponId: {}, shopId: {}", couponId, shopId, e);
      return null;
    }
  }
}
