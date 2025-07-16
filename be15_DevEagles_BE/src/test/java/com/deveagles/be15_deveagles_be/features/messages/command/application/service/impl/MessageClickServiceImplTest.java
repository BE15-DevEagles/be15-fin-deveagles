package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLink;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageClickLinkLog;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageClickLinkLogRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageClickLinkRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("MessageClickServiceImpl 테스트")
@ExtendWith(MockitoExtension.class)
class MessageClickServiceImplTest {

  @InjectMocks private MessageClickServiceImpl messageClickService;

  @Mock private MessageClickLinkRepository linkRepository;

  @Mock private MessageClickLinkLogRepository logRepository;

  @BeforeEach
  void setUp() {
    // @Value 주입이므로 reflection으로 세팅
    ReflectionTestUtils.setField(
        messageClickService, "baseTrackingUrl", "http://localhost:8080/api/v1/track");
  }

  @Test
  @DisplayName("추적 링크 생성 - UUID 포함 링크 반환")
  void createTrackableLink_returnsTrackingUrl() {
    // given
    Long messageId = 1L;
    String originalUrl = "http://yourapp.com/profile/1";

    ArgumentCaptor<MessageClickLink> linkCaptor = ArgumentCaptor.forClass(MessageClickLink.class);

    // when
    String result = messageClickService.createTrackableLink(messageId, originalUrl);

    // then
    verify(linkRepository).save(linkCaptor.capture());
    MessageClickLink savedLink = linkCaptor.getValue();

    assertNotNull(savedLink.getShortToken());
    assertEquals(originalUrl, savedLink.getOriginalUrl());
    assertEquals(messageId, savedLink.getMessageId());
    assertTrue(result.startsWith("http://localhost:8080/api/v1/track/profile?token="));
    assertTrue(result.contains(savedLink.getShortToken()));
  }

  @Test
  @DisplayName("추적 링크 클릭 등록 및 리다이렉트 URL 반환")
  void registerClickAndGetRedirectUrl_registersLogAndReturnsOriginalUrl() {
    // given
    String token = "abc123";
    MessageClickLink link =
        MessageClickLink.builder()
            .id(99L)
            .shortToken(token)
            .originalUrl("http://yourapp.com/profile/99")
            .messageId(1L)
            .build();

    when(linkRepository.findByShortToken(token)).thenReturn(Optional.of(link));
    when(logRepository.existsByMessageLinkId(link.getId())).thenReturn(false);

    // when
    String result = messageClickService.registerClickAndGetRedirectUrl(token);

    // then
    assertEquals("http://yourapp.com/profile/99", result);
    verify(logRepository).save(any(MessageClickLinkLog.class));
  }

  @Test
  @DisplayName("토큰이 유효하지 않으면 예외 발생")
  void registerClickAndGetRedirectUrl_invalidToken_throwsException() {
    // given
    String token = "invalid";
    when(linkRepository.findByShortToken(token)).thenReturn(Optional.empty());

    // expect
    assertThrows(
        BusinessException.class,
        () -> {
          messageClickService.registerClickAndGetRedirectUrl(token);
        });
  }
}
