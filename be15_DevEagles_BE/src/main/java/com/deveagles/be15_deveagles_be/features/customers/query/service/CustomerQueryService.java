package com.deveagles.be15_deveagles_be.features.customers.query.service;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.TagResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.request.CustomerSearchQuery;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerQueryService {

  // 기본 조회
  Optional<CustomerResponse> getCustomerByPhoneNumber(String phoneNumber, Long shopId);

  List<CustomerResponse> getCustomersByShopId(Long shopId);

  long getCustomerCountByShopId(Long shopId);

  boolean existsByPhoneNumber(String phoneNumber, Long shopId);

  // 상세 조회 (조인)
  Optional<CustomerDetailResponse> getCustomerDetail(Long customerId, Long shopId);

  // 목록 조회 (조인 + 페이징)
  List<CustomerListResponse> getCustomerList(Long shopId);

  Page<CustomerListResponse> getCustomerListPaged(Long shopId, Pageable pageable);

  // 검색 (Elasticsearch + JPA 폴백)
  List<CustomerSearchResult> searchByKeyword(String keyword, Long shopId);

  PagedResult<CustomerSearchResult> advancedSearch(CustomerSearchQuery query);

  List<String> autocomplete(String prefix, Long shopId);

  long countByKeyword(String keyword, Long shopId);

  // 태그 조회
  List<TagResponse> getCustomerTags(Long customerId, Long shopId);

  // Elasticsearch 동기화
  void syncCustomerToElasticsearch(Long customerId);

  void reindexAllCustomers(Long shopId);

  // 추가: 안전한 재인덱싱 메서드들
  void reindexAllCustomersWithReset(Long shopId);

  void reindexAllShopsCustomers();

  void reindexAllShopsCustomersWithReset();

  ReindexStatus getReindexStatus(String taskId);

  List<String> getCustomerPhoneNumbers(List<Long> customerIds);

  Optional<CustomerIdResponse> findCustomerIdByPhoneNumber(String phoneNumber, Long shopId);

  // 세그먼트별 고객 조회
  SegmentCustomersResponse getCustomersBySegmentTag(String segmentTag, Long shopId);

  SegmentCustomersResponse getCustomersBySegmentId(Long segmentId, Long shopId);

  List<SegmentCustomersResponse> getCustomersByMultipleSegmentTags(
      List<String> segmentTags, Long shopId);

  List<CustomerResponse> getUnregisteredCustomers(Long shopId);
}
