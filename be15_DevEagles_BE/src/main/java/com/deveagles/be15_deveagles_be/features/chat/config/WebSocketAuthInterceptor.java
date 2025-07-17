package com.deveagles.be15_deveagles_be.features.chat.config;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.common.jwt.JwtTokenProvider;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      log.info("📩 WebSocket CONNECT 요청 도착");

      String token = extractTokenFromHeaders(accessor);
      log.info("📩 헤더에서 추출한 토큰: {}", token);

      if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
        throw new BusinessException(ErrorCode.WEBSOCKET_INVALID_TOKEN);
      }

      try {
        String username = jwtTokenProvider.getUsernameFromJWT(token);
        CustomUser userDetails = (CustomUser) userDetailsService.loadUserByUsername(username);

        // WebSocket 사용자 인증 성공 시 Principal 설정
        Principal principal = () -> String.valueOf(userDetails.getUserId());
        accessor.setUser(principal);

        log.info(" WebSocket 인증 성공: userId={}", userDetails.getUserId());
      } catch (Exception e) {
        throw new BusinessException(ErrorCode.WEBSOCKET_AUTHENTICATION_FAILED);
      }
    }

    return message;
  }

  private String extractTokenFromHeaders(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    String tokenParam = accessor.getFirstNativeHeader("token");
    return StringUtils.hasText(tokenParam) ? tokenParam : null;
  }
}
