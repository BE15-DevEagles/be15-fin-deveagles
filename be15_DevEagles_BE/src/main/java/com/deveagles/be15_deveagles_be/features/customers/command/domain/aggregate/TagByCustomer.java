package com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tag_by_customer")
@IdClass(TagByCustomer.TagByCustomerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TagByCustomer {

  @Id
  @Column(name = "customer_id")
  private Long customerId;

  @Id
  @Column(name = "tag_id")
  private Long tagId;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TagByCustomerId implements Serializable {
    private Long customerId;
    private Long tagId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TagByCustomerId that = (TagByCustomerId) o;
      return Objects.equals(customerId, that.customerId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(customerId, tagId);
    }
  }
}
