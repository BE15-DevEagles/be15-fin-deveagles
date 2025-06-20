package com.deveagles.be15_deveagles_be.features.customers.query.repository;

import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import java.util.Optional;

public interface CustomerDetailQueryRepository {

  Optional<CustomerDetailResponse> findCustomerDetailById(Long customerId, Long shopId);
}
