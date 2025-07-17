package com.deveagles.be15_deveagles_be.features.schedules.query.dto.response;

import java.time.LocalDateTime;

public record ReservationDetailResponse(
    Long reservationId,
    Long customerId,
    String customerName,
    String customerPhone,
    LocalDateTime reservationStartAt,
    LocalDateTime reservationEndAt,
    String staffName,
    String reservationStatusName,
    String staffMemo,
    String reservationMemo,
    String itemNames,
    String secondaryItemIds) {}
