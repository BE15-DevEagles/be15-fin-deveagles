package com.deveagles.be15_deveagles_be.features.messages.command.application.controller;

import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageClickService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageClickRedirectController {
  private final MessageClickService messageClickService;

  @GetMapping("/track/profile")
  public ResponseEntity<Void> redirectToProfile(@RequestParam("token") String token) {
    String redirectUrl = messageClickService.registerClickAndGetRedirectUrl(token);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
  }
}
