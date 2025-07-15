package com.deveagles.be15_deveagles_be.features.membership.query.dto.response;

import java.util.Date;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerSessionPassReponse {

  private Long customerSessionPassId;
  private Integer remainingCount;
  private Integer totalCount;
  private Long secondaryItemId;
  private String sessionPassName;
  private Date expirationDate;
}
