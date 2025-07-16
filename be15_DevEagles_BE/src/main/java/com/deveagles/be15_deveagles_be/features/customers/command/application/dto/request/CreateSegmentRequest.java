package com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSegmentRequest {
  @NotBlank(message = "세그먼트 태그는 필수입니다.")
  @Size(max = 20, message = "세그먼트 태그는 20자 이하여야 합니다.")
  private String segmentTag;

  @NotBlank(message = "세그먼트 타이틀은 필수입니다.")
  @Size(max = 30, message = "세그먼트 타이틀은 30자 이하여야 합니다.")
  private String segmentTitle;

  @Size(max = 20, message = "색상코드는 20자 이하여야 합니다.")
  private String colorCode;
}
