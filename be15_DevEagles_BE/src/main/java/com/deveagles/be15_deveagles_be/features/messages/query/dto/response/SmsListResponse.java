package com.deveagles.be15_deveagles_be.features.messages.query.dto.response;

import java.time.LocalDateTime;

public record SmsListResponse(
    Long messageId,
    String title,
    String content,
    String receiverName,
    String statusLabel,
    LocalDateTime date, // 예약 or 발송 일자
    boolean canEdit,
    boolean canDelete,
    String errorMessage,
    String messageSendingType) {}
