package com.deveagles.be15_deveagles_be.features.customers.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.SegmentByCustomer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentByCustomerRepository
    extends JpaRepository<SegmentByCustomer, SegmentByCustomer.SegmentByCustomerId> {

  List<SegmentByCustomer> findByCustomerId(Long customerId);

  List<SegmentByCustomer> findBySegmentId(Long segmentId);

  List<SegmentByCustomer> findBySegmentIdIn(List<Long> segmentIds);

  boolean existsByCustomerIdAndSegmentId(Long customerId, Long segmentId);

  @Modifying
  @Query(
      "DELETE FROM SegmentByCustomer sbc WHERE sbc.customerId = :customerId AND sbc.segmentId = :segmentId")
  void deleteByCustomerIdAndSegmentId(
      @Param("customerId") Long customerId, @Param("segmentId") Long segmentId);

  @Modifying
  @Query("DELETE FROM SegmentByCustomer sbc WHERE sbc.customerId = :customerId")
  void deleteByCustomerId(@Param("customerId") Long customerId);

  @Modifying
  @Query(
      "DELETE FROM SegmentByCustomer sbc WHERE sbc.customerId = :customerId AND sbc.segmentId IN "
          + "(SELECT s.id FROM Segment s WHERE s.segmentTag LIKE %:keyword%)")
  void deleteRiskSegmentsByCustomerId(
      @Param("customerId") Long customerId, @Param("keyword") String keyword);

  @Query(
      "SELECT sbc FROM SegmentByCustomer sbc JOIN Segment s ON sbc.segmentId = s.id "
          + "WHERE sbc.customerId = :customerId AND s.segmentTag LIKE %:keyword%")
  List<SegmentByCustomer> findRiskSegmentsByCustomerId(
      @Param("customerId") Long customerId, @Param("keyword") String keyword);

  @Query("SELECT sbc.customerId FROM SegmentByCustomer sbc WHERE sbc.segmentId = :segmentId")
  List<Long> findCustomerIdsBySegmentId(@Param("segmentId") Long segmentId);

  @Query(
      "SELECT sbc.customerId FROM SegmentByCustomer sbc JOIN Segment s ON sbc.segmentId = s.id "
          + "WHERE s.segmentTag = :segmentTag")
  List<Long> findCustomerIdsBySegmentTag(@Param("segmentTag") String segmentTag);

  @Query(
      "SELECT sbc.customerId FROM SegmentByCustomer sbc JOIN Segment s ON sbc.segmentId = s.id "
          + "WHERE s.segmentTag IN :segmentTags")
  List<Long> findCustomerIdsBySegmentTags(@Param("segmentTags") List<String> segmentTags);

  @Query(
      "SELECT sbc FROM SegmentByCustomer sbc JOIN Segment s ON sbc.segmentId = s.id "
          + "WHERE sbc.customerId = :customerId AND s.segmentTag IN ('new', 'growing', 'loyal', 'vip', 'inactive')")
  List<SegmentByCustomer> findLifecycleSegmentsByCustomerId(@Param("customerId") Long customerId);

  @Modifying
  @Query(
      "DELETE FROM SegmentByCustomer sbc WHERE sbc.segmentId IN "
          + "(SELECT s.id FROM Segment s WHERE s.segmentTag IN ('new', 'growing', 'loyal', 'vip', 'dormant', 'new_followup', 'new_at_risk', 'reactivation_needed', 'growing_delayed', 'loyal_delayed'))")
  void deleteAllLifecycleSegments();
}
