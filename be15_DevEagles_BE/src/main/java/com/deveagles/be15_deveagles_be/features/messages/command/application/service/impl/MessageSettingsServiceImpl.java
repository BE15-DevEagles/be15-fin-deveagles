package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageSettingRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSettingResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageSettingsService;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSettings;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class MessageSettingsServiceImpl implements MessageSettingsService {
  private final MessageSettingRepository messageSettingRepository;

  @Override
  public Long createDefault(Long shopId) {
    if (messageSettingRepository.existsByShopId(shopId)) {
      throw new BusinessException(ErrorCode.MESSAGE_SETTINGS_ALREADY_EXISTS);
    }

    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber(null)
            .canAlimtalk(false)
            .point(0L)
            .build();

    return messageSettingRepository.save(settings).getShopId();
  }

  @Override
  @Transactional(readOnly = true)
  public MessageSettingResponse loadSettings(Long shopId) {
    MessageSettings settings =
        messageSettingRepository
            .findByShopId(shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_SETTINGS_NOT_FOUND));
    return MessageSettingResponse.from(settings);
  }

  @Override
  public void updateSettings(Long shopId, MessageSettingRequest request) {
    MessageSettings settings =
        messageSettingRepository
            .findByShopId(shopId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_SETTINGS_NOT_FOUND));

    settings.update(request.getSenderNumber(), request.getCanAlimtalk());

    if (request.getPoint() != null && request.getPoint() > 0) {
      settings.addPoint(request.getPoint());
    }
  }
}
