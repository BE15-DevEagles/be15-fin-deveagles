package com.deveagles.be15_deveagles_be.common.config;

import com.deveagles.be15_deveagles_be.common.jwt.JwtAuthenticationFilter;
import com.deveagles.be15_deveagles_be.common.jwt.JwtTokenProvider;
import com.deveagles.be15_deveagles_be.common.jwt.RestAccessDeniedHandler;
import com.deveagles.be15_deveagles_be.common.jwt.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  private final RestAccessDeniedHandler restAccessDeniedHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/notifications/subscribe")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/auth/login",
                        "/auth/check-email",
                        "/auth/refresh",
                        "/auth/verify",
                        "/users",
                        "/users/valid-id",
                        "/users/valid-email",
                        "/shops/valid-biz",
                        "/schedules/reservations")
                    .permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/users/password")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/shops/get-industry",
                        "/schedules/reservation/settings",
                        "/schedules/reservation/settings/{shopId}",
                        "/shops/p/{shopId}",
                        "/staffs/{staffId}",
                        "/secondary-items/active",
                        "/primary-items",
                        "/schedules/reservations/staff/{staffId}/available-times")
                    .permitAll()
                    .requestMatchers(
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-ui/index.html",
                        "/swagger-resources/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/ws-chat/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
  }
}
