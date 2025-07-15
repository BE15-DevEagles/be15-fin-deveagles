package com.deveagles.be15_deveagles_be.features.messages.query.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.messages.query.dto.response.SmsDetailResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.dto.response.SmsListResponse;
import com.deveagles.be15_deveagles_be.features.messages.query.service.SmsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메시지 조회", description = "메시지 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/message")
public class SmsQueryController {

  private final SmsQueryService smsQueryService;

  @Operation(summary = "문자 발송 내역 목록 조회", description = "현재 로그인한 매장의 문자 발송 내역을 페이지네이션 형태로 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResult<SmsListResponse>>> getSmsList(
      @AuthenticationPrincipal CustomUser customUser,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    System.out.println(customUser.getShopId());
    PagedResult<SmsListResponse> result =
        smsQueryService.getSmsList(customUser.getShopId(), pageable);
    System.out.println("📦 [백엔드 응답 디버깅]");
    System.out.println("→ totalItems = " + result.getPagination().getTotalItems());
    System.out.println("→ totalPages = " + result.getPagination().getTotalPages());
    System.out.println("→ currentPage = " + result.getPagination().getCurrentPage());
    System.out.println("→ content.size() = " + result.getContent().size());
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "문자 발송 상세 조회", description = "특정 메시지 ID를 기준으로 문자 발송 상세 내용을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "메시지를 찾을 수 없음")
  })
  @GetMapping("/{messageId}")
  public ResponseEntity<ApiResponse<SmsDetailResponse>> getSmsDetail(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long messageId) {
    SmsDetailResponse response = smsQueryService.getSmsDetail(customUser.getShopId(), messageId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
