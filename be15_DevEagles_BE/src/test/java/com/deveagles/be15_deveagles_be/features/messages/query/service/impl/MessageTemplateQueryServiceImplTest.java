package com.deveagles.be15_deveagles_be.features.messages.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageTemplateResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplate;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.MessageTemplateType;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.MessageTemplateQueryRepository;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageTemplateQueryServiceImpl 단위 테스트")
class MessageTemplateQueryServiceImplTest {

  @Mock private ShopCommandService shopCommandService;

  @Mock private MessageTemplateQueryRepository messageTemplateQueryRepository;

  @InjectMocks private MessageTemplateQueryServiceImpl queryService;

  @Test
  @DisplayName("템플릿 목록 조회 성공")
  void findAllTemplates_success() {
    // given
    Long shopId = 1L;
    Pageable pageable = PageRequest.of(0, 10);

    MessageTemplate template =
        MessageTemplate.builder()
            .templateId(1L)
            .templateName("환영 메시지")
            .templateContent("안녕하세요 고객님")
            .shopId(shopId)
            .templateType(MessageTemplateType.advertising)
            .build();

    Page<MessageTemplate> page = new PageImpl<>(List.of(template), pageable, 1);
    given(messageTemplateQueryRepository.findAllByShopId(shopId, pageable)).willReturn(page);

    // when
    PagedResult<MessageTemplateResponse> result = queryService.findAll(shopId, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTemplateName()).isEqualTo("환영 메시지");
  }

  @Test
  @DisplayName("템플릿 단건 조회 성공")
  void findOneTemplate_success() {
    // given
    Long shopId = 1L;
    Long templateId = 100L;

    MessageTemplate template =
        MessageTemplate.builder()
            .templateId(templateId)
            .templateName("예약 알림")
            .templateContent("예약이 완료되었습니다.")
            .shopId(shopId)
            .templateType(MessageTemplateType.advertising)
            .build();

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(templateId))
        .willReturn(Optional.of(template));

    // when
    MessageTemplateResponse result = queryService.findOne(shopId, templateId);

    // then
    assertThat(result.getTemplateId()).isEqualTo(templateId);
    assertThat(result.getTemplateContent()).contains("예약");
  }

  @Test
  @DisplayName("템플릿 단건 조회 실패 - 존재하지 않음")
  void findOneTemplate_notFound() {
    // given
    Long shopId = 1L;
    Long templateId = 999L;

    given(messageTemplateQueryRepository.findByIdAndNotDeleted(templateId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> queryService.findOne(shopId, templateId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("템플릿이 존재하지 않습니다.");
  }
}
