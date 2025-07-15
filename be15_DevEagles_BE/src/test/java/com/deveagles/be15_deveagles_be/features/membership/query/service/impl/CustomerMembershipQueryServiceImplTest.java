package com.deveagles.be15_deveagles_be.features.membership.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.features.membership.query.dto.request.CustomerMemebershipFilterRequest;
import com.deveagles.be15_deveagles_be.features.membership.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.membership.query.mapper.CustomerMembershipMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CustomerMembershipQueryServiceImplTest {

  @Mock private CustomerMembershipMapper customerMembershipMapper;

  @InjectMocks private CustomerMembershipQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("getCustomerMembershipList(shopId, page, size) - 정상 흐름")
  void getCustomerMembershipList_withoutFilter_success() {
    Long shopId = 1L;
    int page = 1;
    int size = 10;
    int offset = 0;

    CustomerMembershipResponse customer = mock(CustomerMembershipResponse.class);
    when(customer.getCustomerId()).thenReturn(100L);

    SessionPassInfo sessionPass = mock(SessionPassInfo.class);

    when(customerMembershipMapper.findAllCustomerMemberships(shopId, offset, size))
        .thenReturn(List.of(customer));
    when(customerMembershipMapper.findSessionPassesByCustomerId(100L))
        .thenReturn(List.of(sessionPass));
    when(customerMembershipMapper.countAllCustomerMemberships(shopId)).thenReturn(1L);

    CustomerMembershipResult result = service.getCustomerMembershipList(shopId, page, size);

    assertThat(result).isNotNull();
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(1L);
  }

  @Test
  @DisplayName("getCustomerMembershipList(shopId, filter) - 정상 흐름")
  void getCustomerMembershipList_withFilter_success() {
    Long shopId = 1L;
    CustomerMemebershipFilterRequest filter = mock(CustomerMemebershipFilterRequest.class);

    when(filter.getPage()).thenReturn(1);
    when(filter.getSize()).thenReturn(10);
    when(filter.getOffset()).thenReturn(0);

    CustomerMembershipResponse customer = mock(CustomerMembershipResponse.class);
    when(customer.getCustomerId()).thenReturn(200L);

    when(customerMembershipMapper.findCustomerMemberships(shopId, filter, 0))
        .thenReturn(List.of(customer));
    when(customerMembershipMapper.findSessionPassesByCustomerId(200L))
        .thenReturn(List.of(mock(SessionPassInfo.class)));
    when(customerMembershipMapper.countCustomerMemberships(shopId, filter)).thenReturn(1L);

    CustomerMembershipResult result = service.getCustomerMembershipList(shopId, filter);

    assertThat(result).isNotNull();
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("getPrepaidPassDetailsByCustomerId - 정상")
  void getPrepaidPassDetailsByCustomerId_success() {
    Long customerId = 123L;
    when(customerMembershipMapper.findPrepaidPassDetailsByCustomerId(customerId))
        .thenReturn(List.of(mock(CustomerPrepaidPassDetailInfo.class)));

    List<CustomerPrepaidPassDetailInfo> result =
        service.getPrepaidPassDetailsByCustomerId(customerId);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getSessionPassDetailsByCustomerId - 정상")
  void getSessionPassDetailsByCustomerId_success() {
    Long customerId = 456L;
    when(customerMembershipMapper.findSessionPassDetailsByCustomerId(customerId))
        .thenReturn(List.of(mock(CustomerSessionPassDetailInfo.class)));

    List<CustomerSessionPassDetailInfo> result =
        service.getSessionPassDetailsByCustomerId(customerId);
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getExpiredOrUsedUpMemberships - 정상")
  void getExpiredOrUsedUpMemberships_success() {
    Long customerId = 789L;

    when(customerMembershipMapper.findExpiredOrUsedUpPrepaidPasses(customerId))
        .thenReturn(List.of(mock(CustomerPrepaidPassDetailInfo.class)));
    when(customerMembershipMapper.findExpiredOrUsedUpSessionPasses(customerId))
        .thenReturn(List.of(mock(CustomerSessionPassDetailInfo.class)));

    CustomerExpiringMembershipResult result = service.getExpiredOrUsedUpMemberships(customerId);

    assertThat(result).isNotNull();
    assertThat(result.getPlist()).hasSize(1);
    assertThat(result.getSList()).hasSize(1);
  }

  @Test
  @DisplayName("getAvailableSessionPassesByCustomerId - 정상")
  void getAvailableSessionPassesByCustomerId_success() {
    // given
    Long customerId = 123L;
    CustomerSessionPassReponse mockResponse = mock(CustomerSessionPassReponse.class);
    when(customerMembershipMapper.findUsableSessionPassesByCustomerId(customerId))
        .thenReturn(List.of(mockResponse));

    // when
    List<CustomerSessionPassReponse> result =
        service.getAvailableSessionPassesByCustomerId(customerId);

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
  }
}
