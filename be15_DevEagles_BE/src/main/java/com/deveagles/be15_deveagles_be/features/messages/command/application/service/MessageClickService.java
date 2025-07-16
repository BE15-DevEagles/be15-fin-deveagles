package com.deveagles.be15_deveagles_be.features.messages.command.application.service;

public interface MessageClickService {
  String createTrackableLink(Long messageId, String originalUrl);

  String registerClickAndGetRedirectUrl(String token);
}
