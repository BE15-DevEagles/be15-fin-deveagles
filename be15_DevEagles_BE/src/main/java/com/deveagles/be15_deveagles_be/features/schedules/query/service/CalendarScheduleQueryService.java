package com.deveagles.be15_deveagles_be.features.schedules.query.service;

import com.deveagles.be15_deveagles_be.features.schedules.query.dto.request.CalendarRegularRequest;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.request.CalendarScheduleRequest;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.response.CalendarRegularResponse;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.response.CalendarRenderedResponse;
import com.deveagles.be15_deveagles_be.features.schedules.query.dto.response.CalendarScheduleResponse;
import com.deveagles.be15_deveagles_be.features.schedules.query.mapper.CalendarScheduleQueryMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendarScheduleQueryService {

  private final CalendarScheduleQueryMapper calendarScheduleQueryMapper;

  public List<CalendarScheduleResponse> findSchedules(
      Long shopId, CalendarScheduleRequest request) {
    return calendarScheduleQueryMapper.findSchedules(shopId, request);
  }

  public List<CalendarRenderedResponse> expandRegularSchedules(
      List<CalendarRegularResponse> regulars, LocalDate from, LocalDate to) {
    List<CalendarRenderedResponse> result = new ArrayList<>();

    for (CalendarRegularResponse item : regulars) {
      LocalTime startTime = LocalTime.parse(item.startTime());
      LocalTime endTime = LocalTime.parse(item.endTime());

      // 날짜 루프
      for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
        // 주간 반복 체크
        boolean matchWeekly =
            item.weeklyRepeatDay() != null
                && date.getDayOfWeek().name().equalsIgnoreCase(item.weeklyRepeatDay());

        // 월간 반복 체크
        boolean matchMonthly =
            item.monthlyRepeatDay() != null && date.getDayOfMonth() == item.monthlyRepeatDay();

        if (matchWeekly || matchMonthly) {
          result.add(
              new CalendarRenderedResponse(
                  item.id(),
                  item.scheduleType(),
                  item.title(),
                  LocalDateTime.of(date, startTime),
                  LocalDateTime.of(date, endTime),
                  item.staffName(),
                  item.memo(),
                  item.staffColor()));
        }
      }
    }

    return result;
  }

  public List<CalendarRenderedResponse> getExpandedRegularSchedules(
      Long shopId, CalendarRegularRequest request) {
    List<CalendarRegularResponse> regulars =
        calendarScheduleQueryMapper.findRegularSchedules(shopId, request);

    return expandRegularSchedules(regulars, request.from(), request.to());
  }
}
