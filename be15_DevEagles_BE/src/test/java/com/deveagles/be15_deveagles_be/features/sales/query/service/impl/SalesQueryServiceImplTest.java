package com.deveagles.be15_deveagles_be.features.sales.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResult;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.SalesMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SalesQueryServiceImplTest {

  @Mock private SalesMapper salesMapper;

  @InjectMocks private SalesQueryServiceImpl salesQueryService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getSalesList_shouldReturnFilteredResultAndPagination() {
    // given
    Long shopId = 1L;
    SalesListFilterRequest filter = new SalesListFilterRequest();
    filter.setStartDate(LocalDate.of(2024, 1, 1));
    filter.setEndDate(LocalDate.of(2024, 12, 31));
    filter.setSaleTypes(List.of("ITEMS", "REFUND"));
    filter.setStaffId(100L);
    filter.setPage(1);
    filter.setSize(10);

    SalesListResponse sale1 = new SalesListResponse();
    sale1.setSalesId(1L);
    sale1.setSalesType("ITEMS");
    sale1.setStaffName("홍길동");

    SalesListResponse sale2 = new SalesListResponse();
    sale2.setSalesId(2L);
    sale2.setSalesType("REFUND");
    sale2.setStaffName("김영희");

    List<SalesListResponse> mockList = Arrays.asList(sale1, sale2);

    when(salesMapper.findSalesList(shopId, filter)).thenReturn(mockList);
    when(salesMapper.countSalesList(shopId, filter)).thenReturn(2L);

    // when
    SalesListResult result = salesQueryService.getSalesList(shopId, filter);

    // then
    assertThat(result.getList()).hasSize(2);
    assertThat(result.getList().get(0).getSalesType()).isEqualTo("ITEMS");
    assertThat(result.getList().get(1).getSalesType()).isEqualTo("REFUND");

    Pagination pagination = result.getPagination();
    assertThat(pagination.getCurrentPage()).isEqualTo(1);
    assertThat(pagination.getTotalItems()).isEqualTo(2);
    assertThat(pagination.getTotalPages()).isEqualTo(1);

    verify(salesMapper).findSalesList(shopId, filter);
    verify(salesMapper).countSalesList(shopId, filter);
  }
}
