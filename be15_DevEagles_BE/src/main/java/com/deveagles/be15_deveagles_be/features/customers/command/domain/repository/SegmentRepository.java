package com.deveagles.be15_deveagles_be.features.customers.command.domain.repository;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

  Optional<Segment> findBySegmentTag(String segmentTag);

  Optional<Segment> findBySegmentTitle(String segmentTitle);

  List<Segment> findBySegmentTagContaining(String keyword);

  boolean existsBySegmentTag(String segmentTag);

  boolean existsBySegmentTitle(String segmentTitle);

  @Query("SELECT s FROM Segment s WHERE s.segmentTag IN :tags")
  List<Segment> findBySegmentTagIn(@Param("tags") List<String> tags);

  @Query("SELECT s FROM Segment s WHERE s.segmentTag LIKE %:keyword%")
  List<Segment> findRiskSegments(@Param("keyword") String keyword);

  @Query(
      "SELECT s FROM Segment s WHERE s.segmentTag IN ('new', 'growing', 'loyal', 'vip', 'dormant', 'new_followup', 'new_at_risk', 'reactivation_needed', 'growing_delayed', 'loyal_delayed')")
  List<Segment> findLifecycleSegments();
}
