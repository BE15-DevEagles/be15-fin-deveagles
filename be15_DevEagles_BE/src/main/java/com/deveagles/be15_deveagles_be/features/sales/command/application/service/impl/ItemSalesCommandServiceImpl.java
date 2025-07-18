package com.deveagles.be15_deveagles_be.features.sales.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.membership.command.domain.repository.CustomerPrepaidPassRepository;
import com.deveagles.be15_deveagles_be.features.membership.command.domain.repository.CustomerSessionPassRepository;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.ItemSalesRequest;
import com.deveagles.be15_deveagles_be.features.sales.command.application.dto.request.PaymentsInfo;
import com.deveagles.be15_deveagles_be.features.sales.command.application.service.ItemSalesCommandService;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.aggregate.*;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.CustomerMembershipHistoryRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.ItemSalesRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.PaymentsRepository;
import com.deveagles.be15_deveagles_be.features.sales.command.domain.repository.SalesRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemSalesCommandServiceImpl implements ItemSalesCommandService {

  private final CustomerPrepaidPassRepository customerPrepaidPassRepository;
  private final CustomerSessionPassRepository customerSessionPassRepository;
  private final SalesRepository salesRepository;
  private final PaymentsRepository paymentsRepository;
  private final CustomerRepository customerRepository;
  private final ItemSalesRepository itemSalesRepository;
  private final CustomerMembershipHistoryRepository customerMembershipHistoryRepository;
  private final CustomerQueryService customerQueryService;
  private final AutomaticMessageTriggerService automaticMessageTriggerService;

  @Transactional
  @Override
  public void registItemSales(ItemSalesRequest request) {

    // 유효성 검사
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
    for (PaymentsInfo payment : request.getPayments()) {
      if (payment.getAmount() == null || payment.getAmount() < 0) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTSAMOUNT_REQUIRED);
      }

      if (payment.getPaymentsMethod() == null) {
        throw new BusinessException(ErrorCode.SALES_PAYMENTMETHOD_REQUIRED);
      }

      if (payment.getPaymentsMethod() == PaymentsMethod.PREPAID_PASS
          && payment.getCustomerPrepaidPassId() == null) {
        //        throw new BusinessException(ErrorCode.CUSTOMERPREPAIDPASS_REQUIRED);
      }

      if (payment.getPaymentsMethod() == PaymentsMethod.SESSION_PASS
          && payment.getCustomerSessionPassId() == null) {
        //        throw new BusinessException(ErrorCode.CUSTOMERSESSIONPASS_REQUIRED);
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

    // 2. Payments 저장 및 패스 차감
    for (PaymentsInfo p : request.getPayments()) {
      Payments payment =
          Payments.builder()
              .salesId(sales.getSalesId())
              .paymentsMethod(p.getPaymentsMethod())
              .amount(p.getAmount())
              .build();
      paymentsRepository.save(payment);

      Long savedPaymentsId = payment.getPaymentsId(); // 저장된 결제 ID

      switch (p.getPaymentsMethod()) {
        case PREPAID_PASS -> {
          var pass =
              customerPrepaidPassRepository
                  .findById(p.getCustomerPrepaidPassId())
                  .orElseThrow(
                      () -> new BusinessException(ErrorCode.CUSTOMERPREPAIDPASS_NOT_FOUND));

          pass.useAmount(p.getAmount());

          customerMembershipHistoryRepository.save(
              CustomerMembershipHistory.builder()
                  .salesId(sales.getSalesId())
                  .paymentsId(savedPaymentsId)
                  .customerPrepaidPassId(pass.getCustomerPrepaidPassId())
                  .build());

          //  자동발신 트리거
          Optional<CustomerDetailResponse> optionalCustomer =
              customerQueryService.getCustomerDetail(request.getCustomerId(), request.getShopId());

          optionalCustomer.ifPresent(
              customer -> {
                Map<String, String> payload =
                    Map.of(
                        "고객명", customer.getCustomerName(),
                        "선불권금액", String.valueOf(pass.getRemainingAmount()));
                automaticMessageTriggerService.triggerAutomaticSend(
                    customer, AutomaticEventType.PREPAID_USED, payload);
              });
        }

        case SESSION_PASS -> {
          var pass =
              customerSessionPassRepository
                  .findById(p.getCustomerSessionPassId())
                  .orElseThrow(
                      () -> new BusinessException(ErrorCode.CUSTOMERSESSIONPASS_NOT_FOUND));

          int useCount = p.getUsedCount() != null ? p.getUsedCount() : 1;
          pass.useCount(useCount);

          customerMembershipHistoryRepository.save(
              CustomerMembershipHistory.builder()
                  .salesId(sales.getSalesId())
                  .paymentsId(savedPaymentsId)
                  .customerSessionPassId(pass.getCustomerSessionPassId())
                  .usedCount(useCount)
                  .build());

          Optional<CustomerDetailResponse> optionalCustomer =
              customerQueryService.getCustomerDetail(request.getCustomerId(), request.getShopId());
          optionalCustomer.ifPresent(
              customer -> {
                Map<String, String> payload =
                    Map.of(
                        "고객명", customer.getCustomerName(),
                        "횟수권횟수", String.valueOf(pass.getRemainingCount()));
                automaticMessageTriggerService.triggerAutomaticSend(
                    customer, AutomaticEventType.SESSION_PASS_USED, payload);
              });
        }
      }
    }
    // 3. item_sales 저장
    for (ItemSalesRequest.ItemInfo item : request.getItems()) {
      itemSalesRepository.save(
          ItemSales.builder()
              .salesId(sales.getSalesId())
              .secondaryItemId(item.getSecondaryItemId())
              .quantity(item.getQuantity())
              .discountRate(item.getDiscountRate())
              .couponId(item.getCouponId())
              .build());
    }

    // 4. 고객 정보 갱신
    Customer customer =
        customerRepository
            .findById(request.getCustomerId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    customer.incrementVisitCount();
    customer.addRevenue(request.getTotalAmount());
    customer.updateRecentVisitDate(request.getSalesDate().toLocalDate());
  }

  @Transactional
  @Override
  public void updateItemSales(Long salesId, ItemSalesRequest request) {
    // 1. 기존 Sales 조회
    Sales sales =
        salesRepository
            .findById(salesId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SALES_NOT_FOUND));

    // 1-1. 수정 전 총금액을 로컬 변수에 저장
    int oldTotalAmount = sales.getTotalAmount();

    // 2. 기존 Payments 조회 및 soft delete + 패스 복원
    List<Payments> oldPayments = paymentsRepository.findAllBySalesId(salesId);

    for (Payments old : oldPayments) {
      old.softDelete();

      // customer_membership_history 기반으로 어떤 패스를 썼는지 확인
      customerMembershipHistoryRepository
          .findBySalesIdAndPaymentsId(salesId, old.getPaymentsId())
          .ifPresent(
              history -> {
                if (history.getCustomerPrepaidPassId() != null) {
                  var pass =
                      customerPrepaidPassRepository
                          .findById(history.getCustomerPrepaidPassId())
                          .orElseThrow(
                              () -> new BusinessException(ErrorCode.CUSTOMERPREPAIDPASS_NOT_FOUND));
                  pass.restoreAmount(old.getAmount());
                }

                if (history.getCustomerSessionPassId() != null) {
                  var pass =
                      customerSessionPassRepository
                          .findById(history.getCustomerSessionPassId())
                          .orElseThrow(
                              () -> new BusinessException(ErrorCode.CUSTOMERSESSIONPASS_NOT_FOUND));
                  pass.restoreCount(history.getUsedCount());
                }

                // history도 soft delete
                history.softDelete();
              });
    }

    // 3. Sales 필드 수정
    sales.updateSales(
        request.getCustomerId(),
        request.getStaffId(),
        request.getReservationId(),
        request.getRetailPrice(),
        request.getDiscountRate(),
        request.getDiscountAmount(),
        request.getTotalAmount(),
        request.getSalesMemo(),
        request.getSalesDate());

    // 4. 새로운 Payments 추가 + 패스 차감 + history 저장
    for (PaymentsInfo p : request.getPayments()) {
      Payments payment =
          Payments.builder()
              .salesId(salesId)
              .paymentsMethod(p.getPaymentsMethod())
              .amount(p.getAmount())
              .build();
      paymentsRepository.save(payment);

      // customer_membership_history 저장용 변수
      Long customerPrepaidPassId = null;
      Long customerSessionPassId = null;

      int usedCount = 0;
      switch (p.getPaymentsMethod()) {
        case PREPAID_PASS -> {
          var pass =
              customerPrepaidPassRepository
                  .findById(p.getCustomerPrepaidPassId())
                  .orElseThrow(
                      () -> new BusinessException(ErrorCode.CUSTOMERPREPAIDPASS_NOT_FOUND));
          pass.useAmount(p.getAmount());
          customerPrepaidPassId = pass.getCustomerPrepaidPassId();
        }

        case SESSION_PASS -> {
          var pass =
              customerSessionPassRepository
                  .findById(p.getCustomerSessionPassId())
                  .orElseThrow(
                      () -> new BusinessException(ErrorCode.CUSTOMERSESSIONPASS_NOT_FOUND));
          usedCount = p.getUsedCount() != null ? p.getUsedCount() : 1;
          pass.useCount(usedCount);
          customerSessionPassId = pass.getCustomerSessionPassId();
        }
      }

      customerMembershipHistoryRepository.save(
          CustomerMembershipHistory.builder()
              .salesId(salesId)
              .paymentsId(payment.getPaymentsId())
              .customerPrepaidPassId(customerPrepaidPassId)
              .customerSessionPassId(customerSessionPassId)
              .usedCount(usedCount)
              .build());
    }

    // 5. ItemSales 수정 (여러 개)
    List<ItemSales> itemSalesList = itemSalesRepository.findAllBySalesId(salesId);

    for (int i = 0; i < itemSalesList.size(); i++) {
      ItemSales itemSales = itemSalesList.get(i);
      ItemSalesRequest.ItemInfo item = request.getItems().get(i);

      itemSales.updateItemSales(
          item.getSecondaryItemId(),
          item.getQuantity(),
          item.getDiscountRate(),
          item.getCouponId());
    }

    // 6. 고객 정보 갱신 (금액 차이 반영)
    Customer customer =
        customerRepository
            .findById(request.getCustomerId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

    int amountDifference = request.getTotalAmount() - oldTotalAmount;
    customer.addRevenue(amountDifference);
    customer.updateRecentVisitDate(request.getSalesDate().toLocalDate());
  }

  @Transactional
  @Override
  public void refundItemSales(Long salesId) {
    // 1. 기존 Sales 조회 및 환불 여부 확인
    Sales sales =
        salesRepository
            .findById(salesId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SALES_NOT_FOUND));

    if (sales.getIsRefunded()) {
      throw new BusinessException(ErrorCode.SALES_ALREADY_REFUNDED);
    }

    int oldTotalAmount = sales.getTotalAmount();

    // 2. 기존 Payments 조회 및 soft delete + 패스 복원
    List<Payments> oldPayments = paymentsRepository.findAllBySalesId(salesId);

    for (Payments old : oldPayments) {
      old.softDelete();

      customerMembershipHistoryRepository
          .findBySalesIdAndPaymentsId(salesId, old.getPaymentsId())
          .ifPresent(
              history -> {
                if (history.getCustomerPrepaidPassId() != null) {
                  var pass =
                      customerPrepaidPassRepository
                          .findById(history.getCustomerPrepaidPassId())
                          .orElseThrow(
                              () -> new BusinessException(ErrorCode.CUSTOMERPREPAIDPASS_NOT_FOUND));
                  pass.restoreAmount(old.getAmount());
                }

                if (history.getCustomerSessionPassId() != null) {
                  var pass =
                      customerSessionPassRepository
                          .findById(history.getCustomerSessionPassId())
                          .orElseThrow(
                              () -> new BusinessException(ErrorCode.CUSTOMERSESSIONPASS_NOT_FOUND));
                  Integer usedCount = history.getUsedCount();
                  if (usedCount == null) {
                    throw new BusinessException(ErrorCode.INVALID_MEMBERSHIP_HISTORY);
                  }
                  pass.restoreCount(usedCount);
                }

                history.softDelete();
              });
    }

    // 3. Sales 환불 처리
    sales.setRefunded(true);

    // 4. 고객 정보 롤백
    Customer customer =
        customerRepository
            .findById(sales.getCustomerId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    customer.subtractRevenue(oldTotalAmount);
  }
}
