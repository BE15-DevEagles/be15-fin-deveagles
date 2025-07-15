package com.deveagles.be15_deveagles_be.features.notifications.command.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNoticeRequest(
    @NotNull Long shopId, @NotBlank String title, @NotBlank String content) {}
