package com.deveagles.be15_deveagles_be.features.membership.query.service.impl;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerMemebershipFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.membership.query.mapper.CustomerMembershipMapper;
import com.deveagles.be15_deveagles_be.features.membership.query.service.CustomerMembershipQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerMembershipQueryServiceImpl implements CustomerMembershipQueryService {

  private final CustomerMembershipMapper customerMembershipMapper;

  @Override
  public CustomerMembershipResult getCustomerMembershipList(Long shopId, int page, int size) {
    int offset = (page - 1) * size;

    List<CustomerMembershipResponse> list =
        customerMembershipMapper.findAllCustomerMemberships(shopId, offset, size);

    // 고객별 session pass 추가 조회
    for (CustomerMembershipResponse customer : list) {
      List<SessionPassInfo> sessionPasses =
          customerMembershipMapper.findSessionPassesByCustomerId(customer.getCustomerId());
      customer.setSessionPasses(sessionPasses);
    }

    long totalItems = customerMembershipMapper.countAllCustomerMemberships(shopId);

    Pagination pagination =
        Pagination.builder()
            .currentPage(page)
            .totalPages((int) Math.ceil((double) totalItems / size))
            .totalItems(totalItems)
            .build();

    return CustomerMembershipResult.builder().list(list).pagination(pagination).build();
  }

  @Override
  public CustomerMembershipResult getCustomerMembershipList(
      Long shopId, CustomerMemebershipFilterRequest filter) {
    int offset = filter.getOffset();

    List<CustomerMembershipResponse> list =
        customerMembershipMapper.findCustomerMemberships(shopId, filter, offset);

    for (CustomerMembershipResponse customer : list) {
      List<SessionPassInfo> sessionPasses =
          customerMembershipMapper.findSessionPassesByCustomerId(customer.getCustomerId());
      customer.setSessionPasses(sessionPasses);
    }

    long totalItems = customerMembershipMapper.countCustomerMemberships(shopId, filter);

    Pagination pagination =
        Pagination.builder()
            .currentPage(filter.getPage())
            .totalPages((int) Math.ceil((double) totalItems / filter.getSize()))
            .totalItems(totalItems)
            .build();

    return CustomerMembershipResult.builder().list(list).pagination(pagination).build();
  }

  @Override
  public List<CustomerPrepaidPassDetailInfo> getPrepaidPassDetailsByCustomerId(Long customerId) {
    return customerMembershipMapper.findPrepaidPassDetailsByCustomerId(customerId);
  }

  @Override
  public List<CustomerSessionPassDetailInfo> getSessionPassDetailsByCustomerId(Long customerId) {
    return customerMembershipMapper.findSessionPassDetailsByCustomerId(customerId);
  }

  @Override
  public CustomerExpiringMembershipResult getExpiredOrUsedUpMemberships(Long customerId) {
    List<CustomerPrepaidPassDetailInfo> prepaidList =
        customerMembershipMapper.findExpiredOrUsedUpPrepaidPasses(customerId);

    List<CustomerSessionPassDetailInfo> sessionList =
        customerMembershipMapper.findExpiredOrUsedUpSessionPasses(customerId);

    return CustomerExpiringMembershipResult.builder().Plist(prepaidList).SList(sessionList).build();
  }

  @Override
  public List<CustomerSessionPassReponse> getAvailableSessionPassesByCustomerId(Long customerId) {
    return customerMembershipMapper.findUsableSessionPassesByCustomerId(customerId);
  }
}
