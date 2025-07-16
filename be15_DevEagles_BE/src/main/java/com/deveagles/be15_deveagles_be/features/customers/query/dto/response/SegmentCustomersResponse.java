package com.deveagles.be15_deveagles_be.features.customers.query.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record SegmentCustomersResponse(
    String segmentTag, String segmentTitle, Integer customerCount, List<Long> customerIds) {

  public static SegmentCustomersResponse of(
      String segmentTag, String segmentTitle, List<Long> customerIds) {
    return SegmentCustomersResponse.builder()
        .segmentTag(segmentTag)
        .segmentTitle(segmentTitle)
        .customerCount(customerIds.size())
        .customerIds(customerIds)
        .build();
  }
}
