package com.deveagles.be15_deveagles_be.features.customers.command.application.service;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import java.util.List;

public interface SegmentService {

  void assignSegmentToCustomer(Long customerId, String segmentTag);

  void assignSegmentsToCustomer(Long customerId, List<String> segmentTags);

  void removeSegmentFromCustomer(Long customerId, String segmentTag);

  void removeAllSegmentsFromCustomer(Long customerId);

  void removeRiskSegmentsFromCustomer(Long customerId);

  List<Segment> getCustomerSegments(Long customerId);

  List<Long> getCustomerIdsBySegmentTag(String segmentTag);

  List<Long> getCustomerIdsBySegmentTags(List<String> segmentTags);

  Segment createSegmentIfNotExists(String segmentTag, String segmentTitle, String colorCode);

  void initializeDefaultSegments();
}
