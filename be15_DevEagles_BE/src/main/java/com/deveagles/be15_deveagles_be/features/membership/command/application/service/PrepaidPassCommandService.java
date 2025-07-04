package com.deveagles.be15_deveagles_be.features.membership.command.application.service;

import com.deveagles.be15_deveagles_be.features.membership.command.application.dto.request.PrepaidPassRequest;

public interface PrepaidPassCommandService {
  void registPrepaidPass(PrepaidPassRequest request);

  void updatePrepaidPass(Long prepaidPassId, PrepaidPassRequest request);

  void deletePrepaidPass(Long id);
}
