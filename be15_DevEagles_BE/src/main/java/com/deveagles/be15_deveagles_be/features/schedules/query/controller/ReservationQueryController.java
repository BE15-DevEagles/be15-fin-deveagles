package com.deveagles.be15_deveagles_be.features.schedules.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.request.BookedTimeRequest;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.response.BookedTimeResponse;
import com.deveagles.be15_deveagles_be.features.schedules.query.service.ReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules/reservations")
@Tag(name = "예약 조회", description = "예약 및 시간 관련 조회 API")
public class ReservationQueryController {

  private final ReservationQueryService reservationQueryService;

  @Operation(
      summary = "직원의 예약된 시간 조회",
      description = "특정 날짜 기준으로 직원의 예약된 시간을 조회합니다. 해당 시간은 예약 불가 시간입니다.")
  @GetMapping("/staff/{staffId}/available-times")
  public ResponseEntity<ApiResponse<BookedTimeResponse>> getBookedTimes(
      @PathVariable Long staffId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    BookedTimeRequest request = new BookedTimeRequest(staffId, date);
    BookedTimeResponse data = reservationQueryService.getBookedTimes(request);
    return ResponseEntity.ok(ApiResponse.success(data));
  }
}
