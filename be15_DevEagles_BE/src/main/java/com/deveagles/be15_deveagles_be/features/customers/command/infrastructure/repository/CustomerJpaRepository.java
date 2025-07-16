package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerJpaRepository extends JpaRepository<Customer, Long> {

  Optional<Customer> findByIdAndShopIdAndDeletedAtIsNull(Long id, Long shopId);

  List<Customer> findByShopIdAndDeletedAtIsNull(Long shopId);

  List<Customer> findByShopIdAndDeletedAtIsNull(Long shopId, Pageable pageable);

  Optional<Customer> findByPhoneNumberAndShopIdAndDeletedAtIsNull(String phoneNumber, Long shopId);

  boolean existsByPhoneNumberAndShopIdAndDeletedAtIsNull(String phoneNumber, Long shopId);

  long countByShopIdAndDeletedAtIsNull(Long shopId);

  @Query("SELECT DISTINCT c.shopId FROM Customer c WHERE c.deletedAt IS NULL")
  List<Long> findDistinctShopIds();

  List<Customer> findByShopIdAndCustomerNameInAndDeletedAtIsNull(
      Long shopId, List<String> customerNames);
}
