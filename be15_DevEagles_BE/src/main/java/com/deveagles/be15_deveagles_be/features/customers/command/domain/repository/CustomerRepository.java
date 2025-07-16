package com.deveagles.be15_deveagles_be.features.customers.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

  Customer save(Customer customer);

  Optional<Customer> findById(Long id);

  Optional<Customer> findByIdAndShopId(Long id, Long shopId);

  List<Customer> findByShopIdAndDeletedAtIsNull(Long shopId);

  Optional<Customer> findByPhoneNumberAndShopId(String phoneNumber, Long shopId);

  void delete(Customer customer);

  boolean existsByPhoneNumberAndShopId(String phoneNumber, Long shopId);

  List<Customer> findAll();

  long count();

  List<Customer> findAllWithPagination(int page, int size);
}
