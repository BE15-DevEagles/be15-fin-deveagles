package com.deveagles.be15_deveagles_be.features.customers.query.repository;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerListResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerListQueryRepository {

  List<CustomerListResponse> findCustomerListByShopId(Long shopId);

  Page<CustomerListResponse> findCustomerListByShopId(Long shopId, Pageable pageable);
}
