package com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignSegmentToCustomerRequest {
  @NotNull(message = "고객 ID는 필수입니다.") private Long customerId;

  @NotBlank(message = "세그먼트 태그는 필수입니다.")
  private String segmentTag;
}
