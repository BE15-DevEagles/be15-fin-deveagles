package com.deveagles.be15_deveagles_be.features.sales.query.service.impl;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.ItemSalesQueryMapper;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.PrepaidPassSalesQueryMapper;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.SalesMapper;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.SessionPassSalesQueryMapper;
import com.deveagles.be15_deveagles_be.features.sales.query.service.SalesQueryService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesQueryServiceImpl implements SalesQueryService {

  private final SalesMapper salesMapper;
  private final ItemSalesQueryMapper itemSalesQueryMapper;
  private final PrepaidPassSalesQueryMapper prepaidPassSalesQueryMapper;
  private final SessionPassSalesQueryMapper sessionPassSalesQueryMapper;

  @Override
  public SalesListResult getSalesList(Long shopId, SalesListFilterRequest filter) {
    // 1. 매출만 조회 (중복 없이 정확한 LIMIT 적용)
    List<SalesListResponse> list = salesMapper.findSalesListWithoutPayments(shopId, filter);

    // 2. salesId 추출
    List<Long> salesIds = list.stream().map(SalesListResponse::getSalesId).toList();

    // 3. 결제 목록 조회 후 매핑
    if (!salesIds.isEmpty()) {
      Map<Long, List<PaymentsDTO>> paymentsMap =
          salesMapper.findPaymentsBySalesIds(salesIds).stream()
              .collect(Collectors.groupingBy(PaymentsDTO::getSalesId));
      list.forEach(
          sale -> sale.setPayments(paymentsMap.getOrDefault(sale.getSalesId(), List.of())));
    }

    // 4. 전체 건수 조회
    long totalItems = salesMapper.countSalesList(shopId, filter);
    int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());

    // 5. 반환
    return SalesListResult.builder()
        .list(list)
        .pagination(new Pagination(filter.getPage(), totalPages, totalItems))
        .build();
  }

  @Override
  public ItemSalesDetailResponse getItemSalesDetail(Long itemSalesId) {
    // 기본 정보 조회 (조인된 단일 행)
    ItemSalesDetailResponse detail = itemSalesQueryMapper.findItemSalesDetail(itemSalesId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.SALES_NOT_FOUND);
    }

    // 결제 수단 리스트 조회
    List<PaymentsDTO> payments = itemSalesQueryMapper.findPaymentsBySalesId(detail.getSalesId());
    detail.setPayments(payments);

    return detail;
  }

  @Override
  public PrepaidPassSalesDetailResponse getPrepaidPassSalesDetail(Long prepaidPassSalesId) {
    // 기본 정보 조회
    PrepaidPassSalesDetailResponse detail =
        prepaidPassSalesQueryMapper.findPrepaidPassSalesDetail(prepaidPassSalesId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.SALES_NOT_FOUND);
    }

    // 결제 수단 조회
    List<PaymentsDTO> payments =
        prepaidPassSalesQueryMapper.findPaymentsBySalesId(detail.getSalesId());
    detail.setPayments(payments);

    return detail;
  }

  @Override
  public SessionPassSalesDetailResponse getSessionPassSalesDetail(Long sessionPassSalesId) {
    // 기본 정보 조회
    SessionPassSalesDetailResponse detail =
        sessionPassSalesQueryMapper.findSessionPassSalesDetail(sessionPassSalesId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.SALES_NOT_FOUND);
    }

    // 결제 수단 조회
    List<PaymentsDTO> payments =
        sessionPassSalesQueryMapper.findPaymentsBySalesId(detail.getSalesId());
    detail.setPayments(payments);

    return detail;
  }
}
