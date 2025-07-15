package com.deveagles.be15_deveagles_be.features.messages.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.messages.query.dto.response.SmsDetailResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.dto.response.SmsListResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.repository.SmsQueryRepository;
import java.time.LocalDateTime;
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
@DisplayName("SmsQueryServiceImpl 단위 테스트")
class SmsQueryServiceImplTest {

  @Mock private SmsQueryRepository smsQueryRepository;

  @InjectMocks private SmsQueryServiceImpl smsQueryService;

  @Test
  @DisplayName("문자 목록 조회 성공")
  void getSmsList_success() {
    // given
    Long shopId = 1L;
    Pageable pageable = PageRequest.of(0, 10);

    List<SmsListResponse> content =
        List.of(
            new SmsListResponse(
                1L, "예약", "예약 메시지입니다", "고객A", "SENT", LocalDateTime.now(), true, true, "", "AUTO"));

    // ✅ mockPage 생성
    Page<SmsListResponse> mockPage = new PageImpl<>(content, pageable, content.size());

    given(smsQueryRepository.findSmsListByShopId(shopId, pageable)).willReturn(mockPage);

    // when
    PagedResult<SmsListResponse> result = smsQueryService.getSmsList(shopId, pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).messageId()).isEqualTo(1L);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(1);
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(0);
  }

  @Test
  @DisplayName("문자 상세 조회 성공")
  void getSmsDetail_success() {
    // given
    Long shopId = 1L;
    Long messageId = 42L;

    SmsDetailResponse mockResponse =
        new SmsDetailResponse(
            messageId,
            "내용",
            "SENT",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            "SMS",
            "RESERVATION",
            "예약안내",
            true,
            100L,
            200L,
            300L,
            400L);

    given(smsQueryRepository.findSmsDetailByIdAndShopId(messageId, shopId))
        .willReturn(Optional.of(mockResponse));

    // when
    SmsDetailResponse result = smsQueryService.getSmsDetail(shopId, messageId);

    // then
    assertThat(result.messageId()).isEqualTo(messageId);
    assertThat(result.content()).isEqualTo("내용");
  }

  @Test
  @DisplayName("문자 상세 조회 실패 - 존재하지 않는 메시지 ID")
  void getSmsDetail_notFound() {
    // given
    Long shopId = 1L;
    Long messageId = 999L;

    given(smsQueryRepository.findSmsDetailByIdAndShopId(messageId, shopId))
        .willReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(() -> smsQueryService.getSmsDetail(shopId, messageId));

    // then
    assertThat(thrown)
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> {
              BusinessException ex = (BusinessException) e;
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SMS_NOT_FOUND);
            });
  }
}
