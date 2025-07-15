package com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "segment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Segment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "segment_id")
  private Long id;

  @Column(name = "segment_title", nullable = false, length = 30)
  private String segmentTitle;

  @Column(name = "segment_tag", nullable = false, length = 10)
  private String segmentTag;

  @Column(name = "color_code", nullable = false, length = 20)
  private String colorCode;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "modified_at", nullable = false)
  private LocalDateTime modifiedAt;

  // Business methods
  public void updateSegmentInfo(String segmentTitle, String segmentTag, String colorCode) {
    this.segmentTitle = segmentTitle;
    this.segmentTag = segmentTag;
    this.colorCode = colorCode;
  }

  public boolean isRiskSegment() {
    return segmentTag.contains("risk") || segmentTag.contains("churn");
  }

  public boolean isLifecycleSegment() {
    return segmentTag.equals("new")
        || segmentTag.equals("growing")
        || segmentTag.equals("loyal")
        || segmentTag.equals("vip");
  }
}
