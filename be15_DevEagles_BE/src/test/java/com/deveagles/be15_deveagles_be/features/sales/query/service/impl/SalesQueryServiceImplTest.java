package com.deveagles.be15_deveagles_be.features.sales.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class SalesQueryServiceImplTest {

  @Mock private SalesMapper salesMapper;
  @Mock private ItemSalesQueryMapper itemSalesQueryMapper;
  @Mock private PrepaidPassSalesQueryMapper prepaidPassSalesQueryMapper;
  @Mock private SessionPassSalesQueryMapper sessionPassSalesQueryMapper;

  @InjectMocks private SalesQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("getSalesList - 정상")
  void getSalesList_success() {
    Long shopId = 1L;
    SalesListFilterRequest filter = mock(SalesListFilterRequest.class);
    when(filter.getSize()).thenReturn(10);
    when(filter.getPage()).thenReturn(1);

    SalesListResponse sale = mock(SalesListResponse.class);
    when(sale.getSalesId()).thenReturn(100L);

    PaymentsDTO payment = mock(PaymentsDTO.class);
    when(payment.getSalesId()).thenReturn(100L);

    when(salesMapper.findSalesListWithoutPayments(shopId, filter)).thenReturn(List.of(sale));
    when(salesMapper.findPaymentsBySalesIds(List.of(100L))).thenReturn(List.of(payment));
    when(salesMapper.countSalesList(shopId, filter)).thenReturn(1L);

    SalesListResult result = service.getSalesList(shopId, filter);

    assertThat(result).isNotNull();
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getPagination()).isInstanceOf(Pagination.class);
  }

  //  @Test
  //  @DisplayName("getItemSalesDetail - 정상")
  //  void getItemSalesDetail_success() {
  //    Long itemSalesId = 1L;
  //    Long salesId = 10L;
  //
  //    ItemSalesDetailResponse detail = new ItemSalesDetailResponse();
  //    detail.setSalesId(salesId);
  //
  //    when(itemSalesQueryMapper.findItemSalesDetailBySalesId(itemSalesId)).thenReturn(detail);
  //    when(itemSalesQueryMapper.findItemSalesListBySalesId(salesId))
  //            .thenReturn(List.of(mock(ItemSalesDetailDTO.class)));
  //    when(itemSalesQueryMapper.findPaymentsBySalesId(salesId))
  //            .thenReturn(List.of(mock(PaymentsDTO.class)));
  //
  //    ItemSalesDetailResponse result = service.getItemSalesDetailBySalesId(itemSalesId);
  //
  //    assertThat(result).isNotNull();
  //    assertThat(result.getPayments()).hasSize(1);
  //    assertThat(result.getItems()).hasSize(1); // ✅ 이 줄이 의미 있는 검증이 되려면 위 mock이 필요
  //  }

  @Test
  @DisplayName("getItemSalesDetail - 존재하지 않음")
  void getItemSalesDetail_notFound() {
    when(itemSalesQueryMapper.findItemSalesDetail(anyLong())).thenReturn(null);

    BusinessException ex =
        assertThrows(BusinessException.class, () -> service.getItemSalesDetailBySalesId(999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SALES_NOT_FOUND);
  }

  @Test
  @DisplayName("getPrepaidPassSalesDetail - 정상")
  void getPrepaidPassSalesDetail_success() {
    Long prepaidPassSalesId = 1L;
    Long salesId = 20L;

    PrepaidPassSalesDetailResponse detail = new PrepaidPassSalesDetailResponse();
    detail.setSalesId(salesId);

    when(prepaidPassSalesQueryMapper.findPrepaidPassSalesDetail(prepaidPassSalesId))
        .thenReturn(detail);
    when(prepaidPassSalesQueryMapper.findPaymentsBySalesId(salesId))
        .thenReturn(List.of(mock(PaymentsDTO.class)));

    PrepaidPassSalesDetailResponse result = service.getPrepaidPassSalesDetail(prepaidPassSalesId);

    assertThat(result).isNotNull();
    assertThat(result.getPayments()).hasSize(1);
  }

  @Test
  @DisplayName("getPrepaidPassSalesDetail - 존재하지 않음")
  void getPrepaidPassSalesDetail_notFound() {
    when(prepaidPassSalesQueryMapper.findPrepaidPassSalesDetail(anyLong())).thenReturn(null);

    BusinessException ex =
        assertThrows(BusinessException.class, () -> service.getPrepaidPassSalesDetail(999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SALES_NOT_FOUND);
  }

  @Test
  @DisplayName("getSessionPassSalesDetail - 정상")
  void getSessionPassSalesDetail_success() {
    Long sessionPassSalesId = 1L;
    Long salesId = 30L;

    SessionPassSalesDetailResponse detail = new SessionPassSalesDetailResponse();
    detail.setSalesId(salesId);

    when(sessionPassSalesQueryMapper.findSessionPassSalesDetail(sessionPassSalesId))
        .thenReturn(detail);
    when(sessionPassSalesQueryMapper.findPaymentsBySalesId(salesId))
        .thenReturn(List.of(mock(PaymentsDTO.class)));

    SessionPassSalesDetailResponse result = service.getSessionPassSalesDetail(sessionPassSalesId);

    assertThat(result).isNotNull();
    assertThat(result.getPayments()).hasSize(1);
  }

  @Test
  @DisplayName("getSessionPassSalesDetail - 존재하지 않음")
  void getSessionPassSalesDetail_notFound() {
    when(sessionPassSalesQueryMapper.findSessionPassSalesDetail(anyLong())).thenReturn(null);

    BusinessException ex =
        assertThrows(BusinessException.class, () -> service.getSessionPassSalesDetail(999L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SALES_NOT_FOUND);
  }
}
