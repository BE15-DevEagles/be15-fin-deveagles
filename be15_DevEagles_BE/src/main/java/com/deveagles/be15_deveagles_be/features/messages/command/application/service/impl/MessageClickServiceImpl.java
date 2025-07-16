package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageClickService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLink;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLinkLog;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageClickLinkLogRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageClickLinkRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageClickServiceImpl implements MessageClickService {

  private final MessageClickLinkRepository linkRepository;
  private final MessageClickLinkLogRepository logRepository;

  @Value("${app.tracking.base-url}")
  private String baseTrackingUrl;

  private static final String TRACK_PATH = "/profile?token=";

  @Override
  public String createTrackableLink(Long messageId, String originalUrl) {
    String token = UUID.randomUUID().toString().replace("-", "");
    linkRepository.save(
        MessageClickLink.builder()
            .shortToken(token)
            .originalUrl(originalUrl)
            .messageId(messageId)
            .build());
    return baseTrackingUrl + TRACK_PATH + token;
  }

  @Override
  public String registerClickAndGetRedirectUrl(String token) {
    MessageClickLink link =
        linkRepository
            .findByShortToken(token)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LINK));

    if (!logRepository.existsByMessageLinkId(link.getId())) {
      logRepository.save(MessageClickLinkLog.create(link.getId()));
    }
    return link.getOriginalUrl();
  }
}
