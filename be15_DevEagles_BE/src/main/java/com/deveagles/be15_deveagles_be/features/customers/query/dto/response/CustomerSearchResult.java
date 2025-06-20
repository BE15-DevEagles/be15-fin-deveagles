package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;

public record CustomerSearchResult(
    Long customerId,
    String customerName,
    String phoneNumber,
    Long customerGradeId,
    String customerGradeName,
    Customer.Gender gender) {

  public static CustomerSearchResult from(CustomerDocument document) {
    return new CustomerSearchResult(
        document.getCustomerId(),
        document.getCustomerName(),
        document.getPhoneNumber(),
        document.getCustomerGradeId(),
        document.getCustomerGradeName(),
        document.getGender() != null ? Customer.Gender.valueOf(document.getGender()) : null);
  }

  public static CustomerSearchResult of(
      Long customerId,
      String customerName,
      String phoneNumber,
      Long customerGradeId,
      String customerGradeName,
      Customer.Gender gender) {
    return new CustomerSearchResult(
        customerId, customerName, phoneNumber, customerGradeId, customerGradeName, gender);
  }
}
