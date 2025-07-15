package com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "segment_by_customer")
@IdClass(SegmentByCustomer.SegmentByCustomerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SegmentByCustomer {

  @Id
  @Column(name = "customer_id")
  private Long customerId;

  @Id
  @Column(name = "segment_id")
  private Long segmentId;

  @CreationTimestamp
  @Column(name = "assigned_at", nullable = false, updatable = false)
  private LocalDateTime assignedAt;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SegmentByCustomerId implements Serializable {
    private Long customerId;
    private Long segmentId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SegmentByCustomerId that = (SegmentByCustomerId) o;
      return Objects.equals(customerId, that.customerId)
          && Objects.equals(segmentId, that.segmentId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(customerId, segmentId);
    }
  }
}
