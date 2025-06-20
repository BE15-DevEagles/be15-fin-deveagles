package com.deveagles.be15_deveagles_be.features.customers.query.dto.request;

public record CustomerSearchQuery(
    Long shopId,
    String keyword, // 이름 또는 전화번호로 검색
    Long customerGradeId,
    String gender, // M 또는 F
    Boolean marketingConsent,
    Boolean notificationConsent,
    Boolean includeDeleted,
    int page,
    int size,
    String sortBy,
    String sortDirection) {
  public CustomerSearchQuery {
    if (page < 0) page = 0;
    if (size <= 0) size = 20;
    if (size > 100) size = 100;
    if (sortBy == null) sortBy = "createdAt";
    if (sortDirection == null) sortDirection = "DESC";
    if (includeDeleted == null) includeDeleted = false;
  }
}
