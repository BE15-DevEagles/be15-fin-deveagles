package com.deveagles.be15_deveagles_be.features.messages.command.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateCreateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageTemplateUpdateRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplateType;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.repository.MessageTemplateRepository;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageTemplateServiceImplTest 단위 테스트")
class MessageTemplateServiceImplTest {

  @InjectMocks private MessageTemplateServiceImpl messageTemplateService;

  @Mock private MessageTemplateRepository messageTemplateRepository;

  @Mock private ShopCommandService shopCommandService;

  @Mock private MessageTemplateQueryRepository messageTemplateQueryRepository;

  @Test
  @DisplayName("템플릿 생성 성공")
  void createTemplate_success() {
    // given
    Long shopId = 1L;
    MessageTemplateCreateRequest request =
        new MessageTemplateCreateRequest(
            "환영 메시지", "안녕하세요 #{고객명}님", MessageTemplateType.advertising, 2L, 3L);

    MessageTemplate savedTemplate =
        MessageTemplate.builder()
            .templateId(100L)
            .templateName("환영 메시지")
            .templateContent("안녕하세요 #{고객명}님")
            .templateType(MessageTemplateType.advertising)
            .shopId(shopId)
            .customerGradeId(2L)
            .tagId(3L)
            .build();

    given(messageTemplateRepository.save(any())).willReturn(savedTemplate);

    // when
    MessageTemplateResponse response = messageTemplateService.createTemplate(shopId, request);

    // then
    assertThat(response.getTemplateId()).isEqualTo(100L);
    assertThat(response.getTemplateName()).isEqualTo("환영 메시지");
    verify(shopCommandService).validateShopExists(shopId);
    verify(messageTemplateRepository).save(any());
  }

  @Test
  @DisplayName("템플릿 수정 성공")
  void changeTemplate_success() {
    // given
    Long templateId = 10L;
    Long shopId = 1L;

    MessageTemplate existing = mock(MessageTemplate.class);
    given(existing.isUsableForShop(shopId)).willReturn(true);

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(templateId))
        .willReturn(Optional.of(existing));

    MessageTemplateUpdateRequest request =
        new MessageTemplateUpdateRequest(
            "수정된 이름", "수정된 내용", MessageTemplateType.announcement, 11L, 12L);

    // when
    messageTemplateService.changeTemplate(templateId, shopId, request);

    // then
    verify(existing).update("수정된 이름", "수정된 내용", MessageTemplateType.announcement, 11L, 12L);
  }

  @Test
  @DisplayName("템플릿 수정 실패 - 존재하지 않음")
  void changeTemplate_fail_notFound() {
    // given
    given(messageTemplateQueryRepository.findByIdAndNotDeleted(anyLong()))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () ->
                messageTemplateService.changeTemplate(
                    1L, 1L, mock(MessageTemplateUpdateRequest.class)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.TEMPLATE_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("템플릿 수정 실패 - 접근 불가")
  void changeTemplate_fail_accessDenied() {
    MessageTemplate template = mock(MessageTemplate.class);
    given(template.isUsableForShop(1L)).willReturn(false);

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(1L))
        .willReturn(Optional.of(template));

    assertThatThrownBy(
            () ->
                messageTemplateService.changeTemplate(
                    1L, 1L, mock(MessageTemplateUpdateRequest.class)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.TEMPLATE_ACCESS_DENIED.getMessage());
  }

  @Test
  @DisplayName("템플릿 삭제 성공")
  void removeTemplate_success() {
    // given
    MessageTemplate template = mock(MessageTemplate.class);
    given(template.isUsableForShop(1L)).willReturn(true);

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(1L))
        .willReturn(Optional.of(template));

    // when
    messageTemplateService.removeTemplate(1L, 1L);

    // then
    verify(template).softDelete();
  }

  @Test
  @DisplayName("템플릿 삭제 실패 - 존재하지 않음")
  void removeTemplate_fail_notFound() {
    given(messageTemplateQueryRepository.findByIdAndNotDeleted(anyLong()))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> messageTemplateService.removeTemplate(1L, 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.TEMPLATE_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("템플릿 삭제 실패 - 접근 불가")
  void removeTemplate_fail_accessDenied() {
    MessageTemplate template = mock(MessageTemplate.class);
    given(template.isUsableForShop(1L)).willReturn(false);

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(1L))
        .willReturn(Optional.of(template));

    assertThatThrownBy(() -> messageTemplateService.removeTemplate(1L, 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.TEMPLATE_ACCESS_DENIED.getMessage());
  }
}
