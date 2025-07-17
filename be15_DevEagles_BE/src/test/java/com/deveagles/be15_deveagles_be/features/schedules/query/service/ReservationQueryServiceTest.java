package com.deveagles.be15_deveagles_be.features.schedules.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.common.dto.Pagination;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.request.BookedTimeRequest;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.request.ReservationSearchRequest;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.response.*;
import com.deveagles.be15_deveagles_be.features.schedules.query.mapper.ReservationQueryMapper;
import com.deveagles.be15_deveagles_be.features.schedules.query.mapper.ReservationSearchMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 조회 서비스 테스트")
class ReservationQueryServiceTest {

  @Mock private ReservationQueryMapper reservationQueryMapper;
  @Mock private ReservationSearchMapper reservationSearchMapper;

  @InjectMocks private ReservationQueryService reservationQueryService;

  @Test
  void 예약된_시간목록을_조회할_수_있다() {
    // given
    BookedTimeRequest request = new BookedTimeRequest(2L, LocalDate.of(2025, 7, 1));
    List<BookedTimeDto> mockResult =
        List.of(new BookedTimeDto("10:00"), new BookedTimeDto("13:30"));

    when(reservationQueryMapper.findBookedTimes(request)).thenReturn(mockResult);

    // when
    BookedTimeResponse result = reservationQueryService.getBookedTimes(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.bookedTimes()).hasSize(2);
    assertThat(result.bookedTimes().get(0).time()).isEqualTo("10:00");

    verify(reservationQueryMapper, times(1)).findBookedTimes(request);
  }

  @Test
  void 예약_목록을_조회할_수_있다() {
    // given
    Long shopId = 1L;
    int page = 0;
    int size = 10;

    ReservationSearchRequest request =
        ReservationSearchRequest.builder()
            .staffId(2L)
            .reservationStatusName("CONFIRMED")
            .customerKeyword("김")
            .from(LocalDate.of(2025, 6, 1))
            .to(LocalDate.of(2025, 6, 30))
            .build();

    List<ReservationSearchResponse> content =
        List.of(
            new ReservationSearchResponse(
                1L,
                "김민수",
                "010-1234-5678",
                LocalDateTime.of(2025, 6, 15, 10, 0),
                LocalDateTime.of(2025, 6, 15, 11, 0),
                "컷트",
                "담당자A",
                "CONFIRMED",
                "비고1",
                "고객메모1"));

    when(reservationSearchMapper.findReservations(any(), eq(0), eq(10))).thenReturn(content);
    when(reservationSearchMapper.countReservations(any())).thenReturn(1);

    // when
    PagedResult<ReservationSearchResponse> result =
        reservationQueryService.searchReservations(shopId, request, page, size);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getPagination()).isInstanceOf(Pagination.class);
    verify(reservationSearchMapper, times(1)).findReservations(any(), eq(0), eq(10));
    verify(reservationSearchMapper, times(1)).countReservations(any());
  }

  @Test
  void 예약_상세정보를_조회할_수_있다() {
    // given
    Long reservationId = 1L;
    Long shopId = 1L;

    ReservationDetailResponse mockResponse =
        new ReservationDetailResponse(
            reservationId,
            1L,
            "김민수",
            "010-1234-5678",
            LocalDateTime.of(2025, 7, 1, 10, 0),
            LocalDateTime.of(2025, 7, 1, 11, 0),
            "담당자A",
            "CONFIRMED",
            "스태프메모",
            "고객메모",
            "컷트",
            "1, 2");

    when(reservationQueryMapper.findReservationDetailById(reservationId, shopId))
        .thenReturn(mockResponse);

    // when
    ReservationDetailResponse result =
        reservationQueryService.getReservationDetail(reservationId, shopId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.customerName()).isEqualTo("김민수");
    verify(reservationQueryMapper, times(1)).findReservationDetailById(reservationId, shopId);
  }

  @Test
  void 예약_상세정보가_없으면_예외가_발생한다() {
    // given
    Long invalidReservationId = 999L;
    Long shopId = 1L;

    when(reservationQueryMapper.findReservationDetailById(invalidReservationId, shopId))
        .thenReturn(null);

    // when
    Throwable thrown =
        catchThrowable(
            () -> reservationQueryService.getReservationDetail(invalidReservationId, shopId));

    // then
    assertThat(thrown)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
  }
}
