package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageSettingRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSettingResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageSettings;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageSettingRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageSettingsService 단위 테스트")
class MessageSettingsCommandServiceImplTest {

  @InjectMocks private MessageSettingsCommandServiceImpl messageSettingsService;

  @Mock private MessageSettingRepository messageSettingRepository;
  @Mock private ShopCommandServiceImpl shopCommandServiceImpl;

  private final Long shopId = 1L;

  @Test
  @DisplayName("기본 설정 생성 - 이미 존재하면 예외 발생")
  void createDefault_shouldThrowException_whenSettingsExist() {
    given(messageSettingRepository.existsByShopId(shopId)).willReturn(true);

    assertThrows(BusinessException.class, () -> messageSettingsService.createDefault(shopId));
    verify(messageSettingRepository, never()).save(any());
  }

  @Test
  @DisplayName("기본 설정 생성 - 설정 없으면 정상 저장")
  void createDefault_shouldSaveDefaultSettings_whenNotExist() {
    given(messageSettingRepository.existsByShopId(shopId)).willReturn(false);
    given(messageSettingRepository.save(any()))
        .willAnswer(
            invocation -> {
              MessageSettings settings = invocation.getArgument(0);
              return MessageSettings.builder()
                  .shopId(settings.getShopId())
                  .senderNumber(settings.getSenderNumber())
                  .canAlimtalk(settings.isCanAlimtalk())
                  .point(settings.getPoint())
                  .build();
            });

    Long result = messageSettingsService.createDefault(shopId);

    assertEquals(shopId, result);
    verify(messageSettingRepository).save(any(MessageSettings.class));
  }

  @Test
  @DisplayName("설정 조회 - 설정 존재 시 정상 반환")
  void loadSettings_shouldReturnResponse_whenExist() {
    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("01012345678")
            .canAlimtalk(true)
            .point(100L)
            .build();

    given(messageSettingRepository.findByShopId(shopId)).willReturn(Optional.of(settings));

    MessageSettingResponse response = messageSettingsService.loadSettings(shopId);

    assertNotNull(response);
    assertEquals("01012345678", response.getSenderNumber());
    assertTrue(response.isCanAlimtalk());
    assertEquals(100L, response.getPoint());
  }

  @Test
  @DisplayName("설정 조회 - 설정 없으면 예외 발생")
  void loadSettings_shouldThrowException_whenNotFound() {
    given(messageSettingRepository.findByShopId(shopId)).willReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> messageSettingsService.loadSettings(shopId));
  }

  @Test
  @DisplayName("설정 수정 - 필드 정상 업데이트")
  void updateSettings_shouldUpdateFields_whenRequestValid() {
    MessageSettings settings =
        MessageSettings.builder()
            .shopId(shopId)
            .senderNumber("01011112222")
            .canAlimtalk(false)
            .point(500L)
            .build();

    MessageSettingRequest request =
        MessageSettingRequest.builder()
            .senderNumber("01099998888")
            .canAlimtalk(true)
            .point(300L)
            .build();

    given(messageSettingRepository.findByShopId(shopId)).willReturn(Optional.of(settings));

    messageSettingsService.updateSettings(shopId, request);

    assertEquals("01099998888", settings.getSenderNumber());
    assertTrue(settings.isCanAlimtalk());
    assertEquals(800L, settings.getPoint());
  }

  @Test
  @DisplayName("설정 수정 - 설정 없으면 예외 발생")
  void updateSettings_shouldThrowException_whenNotFound() {
    given(messageSettingRepository.findByShopId(shopId)).willReturn(Optional.empty());

    MessageSettingRequest request =
        MessageSettingRequest.builder()
            .senderNumber("01000000000")
            .canAlimtalk(false)
            .point(100L)
            .build();

    assertThrows(
        BusinessException.class, () -> messageSettingsService.updateSettings(shopId, request));
  }
}
