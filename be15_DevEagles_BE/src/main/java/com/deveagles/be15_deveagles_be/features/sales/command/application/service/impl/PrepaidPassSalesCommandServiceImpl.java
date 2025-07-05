package com.deveagles.be15_deveagles_be.features.sales.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PaymentsInfo;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PrepaidPassSalesRequest;
import com.deveagles.be15_deveagles_be.features.sales.command.application.service.PrepaidPassSalesCommandService;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.Payments;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.PrepaidPassSales;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.Sales;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.PaymentsRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.PrepaidPassSalesRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.SalesRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrepaidPassSalesCommandServiceImpl implements PrepaidPassSalesCommandService {

  private final PrepaidPassSalesRepository prepaidPassSalesRepository;
  private final SalesRepository salesRepository;
  private final PaymentsRepository paymentsRepository;

  @Transactional
  @Override
  public void registPrepaidPassSales(PrepaidPassSalesRequest request) {

    if (request.getRetailPrice() < 0) {
      throw new BusinessException(ErrorCode.SALES_RETAILPRICE_REQUIRED);
    }
    if (request.getTotalAmount() < 0) {
      throw new BusinessException(ErrorCode.SALES_TOTALAMOUNT_REQUIRED);
    }
    for (PaymentsInfo payment : request.getPayments()) {
      if (payment.getAmount() == null || payment.getAmount() <= 0) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTSAMOUNT_REQUIRED);
      }
      if (payment.getPaymentsMethod() == null) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTMETHOD_REQUIRED);
      }
    }

    // 1. Sales 저장
    Sales sales =
        Sales.builder()
            .shopId(request.getShopId())
            .customerId(request.getCustomerId())
            .staffId(request.getStaffId())
            .reservationId(request.getReservationId())
            .retailPrice(request.getRetailPrice())
            .discountRate(request.getDiscountRate())
            .discountAmount(request.getDiscountAmount())
            .totalAmount(request.getTotalAmount())
            .salesMemo(request.getSalesMemo())
            .salesDate(request.getSalesDate())
            .isRefunded(false)
            .build();
    salesRepository.save(sales);

    // 2. PrepaidPassSales 저장
    PrepaidPassSales prepaidPassSales =
        PrepaidPassSales.builder()
            .salesId(sales.getSalesId())
            .prepaidPassId(request.getPrepaidPassId())
            .build();
    prepaidPassSalesRepository.save(prepaidPassSales);

    // 3. Payment 저장
    List<Payments> payments =
        request.getPayments().stream()
            .map(
                p ->
                    Payments.builder()
                        .salesId(sales.getSalesId())
                        .paymentsMethod(p.getPaymentsMethod())
                        .amount(p.getAmount())
                        .build())
            .toList();

    for (Payments payment : payments) {
      paymentsRepository.save(payment);
    }
  }

  @Transactional
  @Override
  public void updatePrepaidPassSales(Long salesId, PrepaidPassSalesRequest request) {

    if (request.getRetailPrice() < 0) {
      throw new BusinessException(ErrorCode.SALES_RETAILPRICE_REQUIRED);
    }
    if (request.getTotalAmount() < 0) {
      throw new BusinessException(ErrorCode.SALES_TOTALAMOUNT_REQUIRED);
    }
    for (PaymentsInfo payment : request.getPayments()) {
      if (payment.getAmount() == null || payment.getAmount() <= 0) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTSAMOUNT_REQUIRED);
      }
      if (payment.getPaymentsMethod() == null) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTMETHOD_REQUIRED);
      }
    }

    // 1. 기존 Sales 엔티티 조회 및 업데이트
    Sales sales =
        salesRepository
            .findById(salesId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SALES_NOT_FOUND));

    sales.update(
        request.getShopId(),
        request.getCustomerId(),
        request.getStaffId(),
        request.getReservationId(),
        request.getRetailPrice(),
        request.getDiscountRate(),
        request.getDiscountAmount(),
        request.getTotalAmount(),
        request.getSalesMemo(),
        request.getSalesDate());

    // 2. 기존 PrepaidPassSales 삭제 후 새로 저장
    prepaidPassSalesRepository.deleteBySalesId(salesId);
    PrepaidPassSales prepaidPassSales =
        PrepaidPassSales.builder()
            .salesId(salesId)
            .prepaidPassId(request.getPrepaidPassId())
            .build();
    prepaidPassSalesRepository.save(prepaidPassSales);

    // 3. 기존 Payments 삭제 후 재등록
    paymentsRepository.deleteBySalesId(salesId);

    List<Payments> payments =
        request.getPayments().stream()
            .map(
                p ->
                    Payments.builder()
                        .salesId(salesId)
                        .paymentsMethod(p.getPaymentsMethod())
                        .amount(p.getAmount())
                        .build())
            .toList();

    for (Payments payment : payments) {
      paymentsRepository.save(payment);
    }
  }
}
