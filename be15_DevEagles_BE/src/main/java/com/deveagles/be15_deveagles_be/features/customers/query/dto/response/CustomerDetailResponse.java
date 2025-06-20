package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDetailResponse {

  public CustomerDetailResponse(
      Long customerId,
      String customerName,
      String phoneNumber,
      String memo,
      Integer visitCount,
      Integer totalRevenue,
      LocalDate recentVisitDate,
      LocalDate birthdate,
      LocalDateTime registeredAt,
      Integer noshowCount,
      Gender gender,
      Boolean marketingConsent,
      LocalDateTime marketingConsentedAt,
      Boolean notificationConsent,
      LocalDateTime lastMessageSentAt,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt,
      Long shopId,
      Long staffId,
      String customerGradeName,
      Integer discountRate,
      String channelName) {
    this.customerId = customerId;
    this.customerName = customerName;
    this.phoneNumber = phoneNumber;
    this.memo = memo;
    this.visitCount = visitCount;
    this.totalRevenue = totalRevenue;
    this.recentVisitDate = recentVisitDate;
    this.birthdate = birthdate;
    this.registeredAt = registeredAt;
    this.noshowCount = noshowCount;
    this.gender = gender;
    this.marketingConsent = marketingConsent;
    this.marketingConsentedAt = marketingConsentedAt;
    this.notificationConsent = notificationConsent;
    this.lastMessageSentAt = lastMessageSentAt;
    this.createdAt = createdAt;
    this.modifiedAt = modifiedAt;
    this.shopId = shopId;
    this.staffId = staffId;
    this.customerGradeName = customerGradeName;
    this.discountRate = discountRate;
    this.channelName = channelName;
  }

  private Long customerId;
  private String customerName;
  private String phoneNumber;
  private String memo;
  private Integer visitCount;
  private Integer totalRevenue;
  private LocalDate recentVisitDate;
  private LocalDate birthdate;
  private LocalDateTime registeredAt;
  private Integer noshowCount;
  private Gender gender;
  private Boolean marketingConsent;
  private LocalDateTime marketingConsentedAt;
  private Boolean notificationConsent;
  private LocalDateTime lastMessageSentAt;
  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;

  private Long shopId;
  private Long staffId;
  private String customerGradeName;
  private Integer discountRate;
  private String channelName;

  private List<TagInfo> tags;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TagInfo {
    private Long tagId;
    private String tagName;
    private String colorCode;
  }
}
