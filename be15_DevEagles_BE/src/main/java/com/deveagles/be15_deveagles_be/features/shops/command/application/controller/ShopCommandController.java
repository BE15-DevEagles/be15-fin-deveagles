package com.deveagles.be15_deveagles_be.features.shops.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.shops.command.application.dto.request.PutShopRequest;
import com.deveagles.be15_deveagles_be.features.shops.command.application.dto.request.ValidBizNumberRequest;
import com.deveagles.be15_deveagles_be.features.shops.command.application.dto.response.GetIndustryResponse;
import com.deveagles.be15_deveagles_be.features.shops.command.application.dto.response.GetShopResponse;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops")
@Tag(name = "매장", description = "매장 관련 API")
public class ShopCommandController {

  private final ShopCommandService shopCommandService;

  @PostMapping("/valid-biz")
  public ResponseEntity<ApiResponse<Boolean>> validLoginId(
      @RequestBody @Valid ValidBizNumberRequest validRequest) {

    Boolean is_valid = shopCommandService.validCheckBizNumber(validRequest);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(is_valid));
  }

  @GetMapping("/get-industry")
  public ResponseEntity<ApiResponse<GetIndustryResponse>> getIndustry() {

    GetIndustryResponse response = shopCommandService.getIndustry();

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @GetMapping()
  public ResponseEntity<ApiResponse<GetShopResponse>> getShop(
      @AuthenticationPrincipal CustomUser customUser) {

    GetShopResponse response = shopCommandService.getShop(customUser.getShopId());

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  // 공개 프로필용(인증 불필요) 매장 정보 조회 API ---
  @Transactional(readOnly = true)
  @GetMapping("/p/{shopId}")
  @PreAuthorize("permitAll()") // 로그인하지 않은 사용자도 이 API는 호출 가능하도록 설정
  public ResponseEntity<ApiResponse<GetShopResponse>> getPublicShopInfo(
      @PathVariable Long shopId) { // URL 경로에서 shopId를 변수로 받음

    GetShopResponse response = shopCommandService.getShop(shopId);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @PutMapping()
  public ResponseEntity<ApiResponse<Void>> putShop(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid PutShopRequest shopRequest) {

    shopCommandService.putShop(customUser.getShopId(), shopRequest);

    return ResponseEntity.ok().body(ApiResponse.success(null));
  }
}
