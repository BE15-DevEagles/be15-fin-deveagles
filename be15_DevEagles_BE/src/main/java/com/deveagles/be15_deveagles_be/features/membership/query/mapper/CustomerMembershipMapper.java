package com.deveagles.be15_deveagles_be.features.membership.query.mapper;

import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerExpiringPrepaidPassFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerExpiringSessionPassFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerMemebershipFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.response.*;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerMembershipMapper {
  // 전체 조회용
  List<CustomerMembershipResponse> findAllCustomerMemberships(Long shopId, int offset, int size);

  long countAllCustomerMemberships(Long shopId);

  List<SessionPassInfo> findSessionPassesByCustomerId(Long customerId);

  // 필터 조회용
  List<CustomerMembershipResponse> findCustomerMemberships(
      @Param("shopId") Long shopId,
      @Param("filter") CustomerMemebershipFilterRequest filter,
      @Param("offset") int offset);

  long countCustomerMemberships(
      @Param("shopId") Long shopId, @Param("filter") CustomerMemebershipFilterRequest filter);

  // 고객 선불권 조회 만료(예정)
  List<CustomerExpiringPrepaidPassResponse> findExpiringPrepaidPassCustomers(
      @Param("shopId") Long shopId,
      @Param("request") CustomerExpiringPrepaidPassFilterRequest request,
      @Param("offset") int offset);

  List<PrepaidPassInfo> findExpiringPrepaidPassesByCustomerId(Long customerId);

  long countExpiringPrepaidPassCustomers(
      @Param("shopId") Long shopId,
      @Param("request") CustomerExpiringPrepaidPassFilterRequest request);

  // 고객 횟수권 조회 만료(예정)
  List<CustomerExpiringSessionPassResponse> findExpiringSessionPassCustomers(
      @Param("shopId") Long shopId,
      @Param("request") CustomerExpiringSessionPassFilterRequest request,
      @Param("offset") int offset);

  List<SessionPassInfo> findExpiringSessionPassesByCustomerId(Long customerId);

  long countExpiringSessionPassCustomers(
      @Param("shopId") Long shopId,
      @Param("request") CustomerExpiringSessionPassFilterRequest request);

  // 고객 선불권 상세 조회
  List<CustomerPrepaidPassDetailInfo> findPrepaidPassDetailsByCustomerId(Long customerId);

  // 고객 횟수권 상세 조회
  List<CustomerSessionPassDetailInfo> findSessionPassDetailsByCustomerId(Long customerId);

  List<CustomerPrepaidPassDetailInfo> findExpiredOrUsedUpPrepaidPasses(Long customerId);

  List<CustomerSessionPassDetailInfo> findExpiredOrUsedUpSessionPasses(Long customerId);

  List<CustomerSessionPassReponse> findUsableSessionPassesByCustomerId(Long customerId);
}
