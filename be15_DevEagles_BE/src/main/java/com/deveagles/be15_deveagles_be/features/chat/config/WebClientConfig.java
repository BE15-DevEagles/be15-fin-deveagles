package com.deveagles.be15_deveagles_be.features.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}
