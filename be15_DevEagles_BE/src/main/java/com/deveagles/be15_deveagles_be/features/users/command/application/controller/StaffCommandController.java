package com.deveagles.be15_deveagles_be.features.users.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.CreateStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.PutStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffInfoResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffProfileUrlResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.service.StaffCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/staffs")
@Tag(name = "직원 관리", description = "직원 관리 관련 API")
public class StaffCommandController {

  private final StaffCommandService staffCommandService;

  @PostMapping()
  @Operation(
      summary = "직원 생성",
      description = "새로운 직원을 생성합니다. 프로필 이미지(MultipartFile)도 함께 업로드 가능합니다.")
  public ResponseEntity<ApiResponse<Void>> staffCreate(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestPart @Valid CreateStaffRequest staffRequest,
      @RequestPart(required = false) MultipartFile profile) {

    staffCommandService.staffCreate(customUser.getShopId(), staffRequest, profile);

    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
  }

  @GetMapping("/{staffId}")
  @Operation(
      summary = "직원 상세 조회",
      description = "PathVariable로 전달된 staffId를 기준으로 해당 직원의 상세 정보를 조회합니다.")
  public ResponseEntity<ApiResponse<StaffInfoResponse>> getStaffDetail(
      @AuthenticationPrincipal CustomUser customUser, @PathVariable Long staffId) {

    StaffInfoResponse response = staffCommandService.getStaffDetail(staffId);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @PostMapping("/{staffId}")
  @Operation(
      summary = "직원 정보 수정",
      description =
          "PathVariable로 전달된 staffId를 기준으로 해당 직원 정보를 수정합니다. 프로필 이미지(MultipartFile)도 수정할 수 있습니다.")
  public ResponseEntity<ApiResponse<Void>> putStaffDetail(
      @AuthenticationPrincipal CustomUser customUser,
      @PathVariable Long staffId,
      @RequestPart("staffRequest") @Valid PutStaffRequest staffRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile) {

    staffCommandService.putStaffDetail(staffId, staffRequest, profile);

    return ResponseEntity.ok().body(ApiResponse.success(null));
  }

  @GetMapping("/{staffId}/profile-url")
  @Operation(
      summary = "직원 프로필 URL 조회(고객 예약 전용)",
      description = "PathVariable로 전달된 staffId를 기준으로 해당 직원의 프로필 이미지 URL만 반환합니다.")
  public ResponseEntity<StaffProfileUrlResponse> getStaffProfileUrl(@PathVariable Long staffId) {
    StaffProfileUrlResponse response = staffCommandService.getStaffProfileUrl(staffId);
    return ResponseEntity.ok(response);
  }
}
