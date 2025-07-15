package com.deveagles.be15_deveagles_be.features.notifications.command.application.controller;

import com.deveagles.be15_deveagles_be.common.jwt.JwtTokenProvider;
import com.deveagles.be15_deveagles_be.features.notifications.command.application.service.NotificationSseService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림 실시간 API", description = "SSE를 이용한 실시간 알림 구독 API")
@RestController
@RequestMapping("/notifications") // 기존 조회 컨트롤러와 경로가 겹치지 않도록 주의
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationSseService notificationSseService;
  private final JwtTokenProvider jwtTokenProvider;

  @Operation(summary = "알림 구독", description = "로그인한 사용자가 실시간 알림을 구독합니다. (SSE)")
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@RequestParam("token") String token) {

    if (!jwtTokenProvider.validateToken(token)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }

    Claims claims = jwtTokenProvider.parseClaims(token);

    String shopIdStr = claims.get("shopId", String.class);
    if (shopIdStr == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토큰에 shopId 정보가 없습니다.");
    }
    Long shopId = Long.parseLong(shopIdStr);

    SseEmitter emitter = notificationSseService.subscribe(shopId);
    return ResponseEntity.ok(emitter);
  }
}
