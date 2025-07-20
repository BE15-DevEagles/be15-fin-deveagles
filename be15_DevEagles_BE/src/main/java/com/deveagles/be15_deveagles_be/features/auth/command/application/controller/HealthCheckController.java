package com.deveagles.be15_deveagles_be.features.auth.command.application.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

  @GetMapping("/")
  public String health() {
    return "OK";
  }
}
