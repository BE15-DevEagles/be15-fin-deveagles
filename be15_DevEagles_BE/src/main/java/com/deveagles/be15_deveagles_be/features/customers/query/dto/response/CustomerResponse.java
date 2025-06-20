package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
    Long customerId,
    Long customerGradeId,
    Long shopId,
    Long staffId,
    String customerName,
    String phoneNumber,
    String memo,
    Integer visitCount,
    Integer totalRevenue,
    LocalDate recentVisitDate,
    LocalDate birthdate,
    LocalDateTime registeredAt,
    Integer noshowCount,
    Customer.Gender gender,
    Boolean marketingConsent,
    LocalDateTime marketingConsentedAt,
    Boolean notificationConsent,
    LocalDateTime lastMessageSentAt,
    Long channelId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {
  public static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getCustomerGradeId(),
        customer.getShopId(),
        customer.getStaffId(),
        customer.getCustomerName(),
        customer.getPhoneNumber(),
        customer.getMemo(),
        customer.getVisitCount(),
        customer.getTotalRevenue(),
        customer.getRecentVisitDate(),
        customer.getBirthdate(),
        customer.getRegisteredAt(),
        customer.getNoshowCount(),
        customer.getGender(),
        customer.getMarketingConsent(),
        customer.getMarketingConsentedAt(),
        customer.getNotificationConsent(),
        customer.getLastMessageSentAt(),
        customer.getChannelId(),
        customer.getCreatedAt(),
        customer.getModifiedAt());
  }
}
