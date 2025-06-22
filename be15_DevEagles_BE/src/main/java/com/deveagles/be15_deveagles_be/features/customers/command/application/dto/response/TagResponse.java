package com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {

  private Long tagId;
  private String tagName;
  private String colorCode;
}
