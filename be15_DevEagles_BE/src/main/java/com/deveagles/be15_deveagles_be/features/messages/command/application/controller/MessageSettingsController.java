package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.request.MessageSettingRequest;
import com.deveagles.be15_deveagles_be.features.messages.command.application.dto.response.MessageSettingResponse;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageSettingsCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageSettingsController {
  private final MessageSettingsCommandService messageSettingsCommandService;

  @PostMapping("/{shopId}")
  public ResponseEntity<ApiResponse<Long>> create(@PathVariable Long shopId) {
    Long id = messageSettingsCommandService.createDefault(shopId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shopId));
  }

  @GetMapping("/{shopId}")
  public ResponseEntity<ApiResponse<MessageSettingResponse>> getSettings(
      @PathVariable Long shopId) {
    return ResponseEntity.ok(
        ApiResponse.success(messageSettingsCommandService.loadSettings(shopId)));
  }

  @PutMapping("/{shopId}")
  public ResponseEntity<ApiResponse<Void>> update(
      @PathVariable Long shopId, @Valid @RequestBody MessageSettingRequest request) {
    messageSettingsCommandService.updateSettings(shopId, request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
