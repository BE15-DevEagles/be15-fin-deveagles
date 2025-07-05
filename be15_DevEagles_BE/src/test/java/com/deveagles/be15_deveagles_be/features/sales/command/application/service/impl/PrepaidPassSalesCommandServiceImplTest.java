package com.deveagles.be15_deveagles_be.features.sales.command.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PaymentsInfo;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PrepaidPassSalesRequest;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.PaymentsMethod;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.Sales;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.PaymentsRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.PrepaidPassSalesRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.SalesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrepaidPassSalesCommandServiceImplTest {

  private PrepaidPassSalesRepository prepaidPassSalesRepository;
  private SalesRepository salesRepository;
  private PaymentsRepository paymentsRepository;
  private PrepaidPassSalesCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    prepaidPassSalesRepository = mock(PrepaidPassSalesRepository.class);
    salesRepository = mock(SalesRepository.class);
    paymentsRepository = mock(PaymentsRepository.class);
    service =
        new PrepaidPassSalesCommandServiceImpl(
            prepaidPassSalesRepository, salesRepository, paymentsRepository);
  }

  // ===== 등록 테스트 =====

  @Test
  @DisplayName("실패: 등록 - 정가 음수")
  void registFailsWhenRetailPriceNegative() {
    PrepaidPassSalesRequest request = validRequest();
    request.setRetailPrice(-1);
    assertThrowsWithCode(
        () -> service.registPrepaidPassSales(request), ErrorCode.SALES_RETAILPRICE_REQUIRED);
  }

  @Test
  @DisplayName("실패: 등록 - 총 결제 금액 음수")
  void registFailsWhenTotalAmountNegative() {
    PrepaidPassSalesRequest request = validRequest();
    request.setTotalAmount(-1);
    assertThrowsWithCode(
        () -> service.registPrepaidPassSales(request), ErrorCode.SALES_TOTALAMOUNT_REQUIRED);
  }

  @Test
  @DisplayName("실패: 등록 - 결제 금액이 0 이하")
  void registFailsWhenPaymentAmountZero() {
    PrepaidPassSalesRequest request = validRequest();
    request.setPayments(List.of(new PaymentsInfo(PaymentsMethod.CARD, 0)));
    assertThrowsWithCode(
        () -> service.registPrepaidPassSales(request), ErrorCode.SALES_PAYMENTSAMOUNT_REQUIRED);
  }

  @Test
  @DisplayName("실패: 등록 - 결제 수단 null")
  void registFailsWhenPaymentMethodNull() {
    PrepaidPassSalesRequest request = validRequest();
    request.setPayments(List.of(new PaymentsInfo(null, 1000)));
    assertThrowsWithCode(
        () -> service.registPrepaidPassSales(request), ErrorCode.SALES_PAYMENTMETHOD_REQUIRED);
  }

  // ===== 수정 테스트 =====

  @Test
  @DisplayName("실패: 수정 - 해당 salesId 존재하지 않음")
  void updateFailsWhenSalesNotFound() {
    Long salesId = 123L;
    when(salesRepository.findById(salesId)).thenReturn(Optional.empty());
    assertThrowsWithCode(
        () -> service.updatePrepaidPassSales(salesId, validRequest()), ErrorCode.SALES_NOT_FOUND);
  }

  @Test
  @DisplayName("성공: 수정 - 정상 요청")
  void updateSuccess() {
    Long salesId = 1L;
    PrepaidPassSalesRequest request = validRequest();
    Sales salesMock = mock(Sales.class);

    when(salesRepository.findById(salesId)).thenReturn(Optional.of(salesMock));

    service.updatePrepaidPassSales(salesId, request);

    verify(salesMock)
        .update(
            eq(request.getShopId()),
            eq(request.getCustomerId()),
            eq(request.getStaffId()),
            eq(request.getReservationId()),
            eq(request.getRetailPrice()),
            eq(request.getDiscountRate()),
            eq(request.getDiscountAmount()),
            eq(request.getTotalAmount()),
            eq(request.getSalesMemo()),
            eq(request.getSalesDate()));

    verify(prepaidPassSalesRepository).deleteBySalesId(salesId);
    verify(prepaidPassSalesRepository).save(any());
    verify(paymentsRepository).deleteBySalesId(salesId);
    verify(paymentsRepository, times(1)).save(any());
  }

  // ===== 유틸 =====

  private PrepaidPassSalesRequest validRequest() {
    PrepaidPassSalesRequest request = new PrepaidPassSalesRequest();
    request.setPrepaidPassId(1L);
    request.setCustomerId(2L);
    request.setStaffId(3L);
    request.setShopId(4L);
    request.setRetailPrice(10000);
    request.setTotalAmount(9000);
    request.setSalesDate(LocalDateTime.now());
    request.setPayments(List.of(new PaymentsInfo(PaymentsMethod.CARD, 9000)));
    return request;
  }

  private void assertThrowsWithCode(Runnable runnable, ErrorCode code) {
    BusinessException e = assertThrows(BusinessException.class, runnable::run);
    assertEquals(code, e.getErrorCode());
  }
}
