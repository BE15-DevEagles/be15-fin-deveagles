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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    // 1. 매출만 조회 (중복 포함된 상태로 조회됨)
    List<SalesListResponse> rawSalesList = salesMapper.findSalesListWithoutPayments(shopId, filter);

    // 2. salesId 기준으로 중복 제거하면서 순서 보존
    Map<Long, SalesListResponse> uniqueSalesMap = new LinkedHashMap<>();
    for (SalesListResponse sale : rawSalesList) {
      uniqueSalesMap.putIfAbsent(sale.getSalesId(), sale);
    }
    List<SalesListResponse> salesList = new ArrayList<>(uniqueSalesMap.values());

    // 3. salesId 추출
    List<Long> salesIds = salesList.stream().map(SalesListResponse::getSalesId).toList();

    if (!salesIds.isEmpty()) {
      // 4. 결제 정보 병합
      Map<Long, List<PaymentsDTO>> paymentsMap =
          salesMapper.findPaymentsBySalesIds(salesIds).stream()
              .collect(Collectors.groupingBy(PaymentsDTO::getSalesId));

      // 5. 상품 정보 병합
      Map<Long, List<ItemSalesListDTO>> itemSalesMap =
          salesMapper.findItemSalesBySalesIds(salesIds).stream()
              .collect(Collectors.groupingBy(ItemSalesListDTO::getSalesId));

      // 6. 각 sales에 결제 및 상품 리스트 주입
      for (SalesListResponse sale : salesList) {
        sale.setPayments(paymentsMap.getOrDefault(sale.getSalesId(), List.of()));
        sale.setItems(itemSalesMap.getOrDefault(sale.getSalesId(), List.of()));
      }
    }

    // 7. 전체 건수 조회
    long totalItems = salesMapper.countSalesList(shopId, filter);
    int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());

    // 8. 결과 반환
    return SalesListResult.builder()
        .list(salesList)
        .pagination(new Pagination(filter.getPage(), totalPages, totalItems))
        .build();
  }

  @Override
  public ItemSalesDetailResponse getItemSalesDetailBySalesId(Long salesId) {
    ItemSalesDetailResponse detail = itemSalesQueryMapper.findItemSalesDetailBySalesId(salesId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.SALES_NOT_FOUND);
    }

    List<ItemSalesDetailDTO> items = itemSalesQueryMapper.findItemSalesListBySalesId(salesId);
    List<PaymentsDTO> payments = itemSalesQueryMapper.findPaymentsBySalesId(salesId);

    detail.setItems(items);
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
