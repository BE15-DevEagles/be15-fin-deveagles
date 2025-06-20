package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerListResponse {

  public CustomerListResponse(
      Long customerId,
      String customerName,
      String phoneNumber,
      String memo,
      Integer visitCount,
      Integer totalRevenue,
      LocalDate recentVisitDate,
      LocalDate birthdate,
      String gender,
      String customerGradeName,
      Integer discountRate,
      Long staffId,
      Integer remainingPrepaidAmount) {
    this.customerId = customerId;
    this.customerName = customerName;
    this.phoneNumber = phoneNumber;
    this.memo = memo;
    this.visitCount = visitCount;
    this.totalRevenue = totalRevenue;
    this.recentVisitDate = recentVisitDate;
    this.birthdate = birthdate;
    this.gender = gender;
    this.customerGradeName = customerGradeName;
    this.discountRate = discountRate;
    this.staffId = staffId;
    this.remainingPrepaidAmount = remainingPrepaidAmount;
    this.tags = new ArrayList<>();
  }

  private Long customerId;
  private String customerName;
  private String phoneNumber;
  private String memo;
  private Integer visitCount;
  private Integer totalRevenue;
  private LocalDate recentVisitDate;
  private LocalDate birthdate;
  private String gender;

  private String customerGradeName;
  private Integer discountRate;
  private Long staffId; // TODO: 직원 기능 구현되면 수정 담당자 ID (일단 ID만, 필요하면 담당자명도 조인 가능)
  private Integer remainingPrepaidAmount; // TODO: 선불액 기능 구현되면 수정 잔여선불액 (별도 테이블에서 가져와야 할 수도)

  private List<TagInfo> tags;

  @Getter
  @NoArgsConstructor
  @Builder
  public static class TagInfo {
    private Long tagId;
    private String tagName;
    private String colorCode;

    public TagInfo(Long tagId, String tagName, String colorCode) {
      this.tagId = tagId;
      this.tagName = tagName;
      this.colorCode = colorCode;
    }
  }
}
