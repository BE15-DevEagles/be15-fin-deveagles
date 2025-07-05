package com.deveagles.be15_deveagles_be.features.sales.query.service.impl;

import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.request.SalesListFilterRequest;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResponse;
import com.deveagles.be15_deveagles_be.features.sales.query.dto.response.SalesListResult;
import com.deveagles.be15_deveagles_be.features.sales.query.mapper.SalesMapper;
import com.deveagles.be15_deveagles_be.features.sales.query.service.SalesQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesQueryServiceImpl implements SalesQueryService {

  private final SalesMapper salesMapper;

  @Override
  public SalesListResult getSalesList(Long shopId, SalesListFilterRequest filter) {
    // 1. 매출 목록 조회
    List<SalesListResponse> list = salesMapper.findSalesList(shopId, filter);

    // 2. 전체 건수 조회
    long totalItems = salesMapper.countSalesList(shopId, filter);

    // 3. 페이지 계산
    int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());

    // 4. Pagination 객체 생성
    Pagination pagination =
        Pagination.builder()
            .currentPage(filter.getPage())
            .totalPages(totalPages)
            .totalItems(totalItems)
            .build();

    // 5. 결과 래핑해서 반환
    return SalesListResult.builder().list(list).pagination(pagination).build();
  }
}
