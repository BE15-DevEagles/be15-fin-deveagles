package com.deveagles.be15_deveagles_be.features.customers.command.infrastructure.repository;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {

  private final CustomerJpaRepository jpaRepository;

  @Override
  public Customer save(Customer customer) {
    return jpaRepository.save(customer);
  }

  @Override
  public Optional<Customer> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Optional<Customer> findByIdAndShopId(Long id, Long shopId) {
    return jpaRepository.findByIdAndShopIdAndDeletedAtIsNull(id, shopId);
  }

  @Override
  public List<Customer> findByShopIdAndDeletedAtIsNull(Long shopId) {
    return jpaRepository.findByShopIdAndDeletedAtIsNull(shopId);
  }

  @Override
  public Optional<Customer> findByPhoneNumberAndShopId(String phoneNumber, Long shopId) {
    return jpaRepository.findByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Override
  public void delete(Customer customer) {
    jpaRepository.delete(customer);
  }

  @Override
  public boolean existsByPhoneNumberAndShopId(String phoneNumber, Long shopId) {
    return jpaRepository.existsByPhoneNumberAndShopIdAndDeletedAtIsNull(phoneNumber, shopId);
  }

  @Override
  public List<Customer> findAll() {
    return jpaRepository.findAll();
  }

  @Override
  public long count() {
    return jpaRepository.count();
  }

  @Override
  public List<Customer> findAllWithPagination(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return jpaRepository.findAll(pageable).getContent();
  }
}
