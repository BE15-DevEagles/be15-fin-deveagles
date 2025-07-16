package com.deveagles.be15_deveagles_be.features.customers.command.application.service;

import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.UpdateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.CustomerCommandResponse;

public interface CustomerCommandService {

  CustomerCommandResponse createCustomer(CreateCustomerRequest request);

  CustomerCommandResponse updateCustomer(UpdateCustomerRequest request);

  void deleteCustomer(Long customerId, Long shopId);

  CustomerCommandResponse updateMarketingConsent(Long customerId, Long shopId, Boolean consent);

  CustomerCommandResponse updateNotificationConsent(Long customerId, Long shopId, Boolean consent);

  CustomerCommandResponse addVisit(Long customerId, Long shopId, Integer revenue);

  CustomerCommandResponse addNoshow(Long customerId, Long shopId);

  void createUnknownCustomer(Long shopId, CreateCustomerRequest customer);
}
