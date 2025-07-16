package com.deveagles.be15_deveagles_be.features.users.command.application.controller;

import com.deveagles.be15_deveagles_be.common.dto.ApiResponse;
import com.deveagles.be15_deveagles_be.features.auth.command.application.model.CustomUser;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerGradeRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.request.CreateCustomerRequest;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerCommandService;
import com.deveagles.be15_deveagles_be.features.customers.command.application.service.CustomerGradeCommandService;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageSettingsCommandService;
import com.deveagles.be15_deveagles_be.features.shops.command.application.service.ShopCommandService;
import com.deveagles.be15_deveagles_be.features.shops.command.domain.aggregate.Shop;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.*;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.AccountResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.ProfileResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.service.UserCommandService;
import com.deveagles.be15_deveagles_be.features.users.command.domain.aggregate.Staff;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/users")
@Tag(name = "회원", description = "회원 관련 API")
public class UserCommandController {

  private final UserCommandService userCommandService;
  private final ShopCommandService shopCommandService;
  private final MessageSettingsCommandService messageSettingsCommandService;
  private final CustomerCommandService customerCommandService;
  private final CustomerGradeCommandService customerGradeCommandService;

  @PostMapping()
  public ResponseEntity<ApiResponse<Void>> userCreate(
      @RequestBody @Valid ShopAndUserCreateRequest request) {

    Shop shop = shopCommandService.shopRegist(request.shop());
    Staff staff = userCommandService.userRegist(request.user(), shop.getShopId());
    shopCommandService.patchOwnerId(shop, staff.getStaffId());
    messageSettingsCommandService.createDefault(shop.getShopId());

    CreateCustomerGradeRequest gradeRequest =
        new CreateCustomerGradeRequest(shop.getShopId(), "기본등급", 0);
    Long gradeId = customerGradeCommandService.createCustomerGrade(gradeRequest);

    CreateCustomerRequest customer_f =
        new CreateCustomerRequest(
            gradeId,
            staff.getStaffId(),
            "미등록-여자",
            "01000000000",
            null,
            LocalDate.now(),
            Customer.Gender.F,
            false,
            false,
            5L,
            null);

    customerCommandService.createUnknownCustomer(shop.getShopId(), customer_f);

    CreateCustomerRequest customer_m =
        new CreateCustomerRequest(
            gradeId,
            staff.getStaffId(),
            "미등록-남자",
            "01000000000",
            null,
            LocalDate.now(),
            Customer.Gender.M,
            false,
            false,
            5L,
            null);

    customerCommandService.createUnknownCustomer(shop.getShopId(), customer_m);

    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
  }

  @PostMapping("/valid-id")
  public ResponseEntity<ApiResponse<Boolean>> validLoginId(
      @RequestBody @Valid ValidCheckRequest validRequest) {

    Boolean is_valid = userCommandService.validCheckId(validRequest);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(is_valid));
  }

  @PostMapping("/valid-email")
  public ResponseEntity<ApiResponse<Boolean>> validEmail(
      @RequestBody @Valid ValidCheckRequest validRequest) {

    Boolean is_valid = userCommandService.validCheckEmail(validRequest);

    return ResponseEntity.ok().body(ApiResponse.success(is_valid));
  }

  @PostMapping("/account")
  public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid GetAccountRequest accountRequest) {

    AccountResponse response = userCommandService.getAccount(accountRequest);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @PatchMapping("/account")
  public ResponseEntity<ApiResponse<AccountResponse>> patchAccount(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestBody @Valid PatchAccountRequest accountRequest) {

    AccountResponse response = userCommandService.patchAccount(accountRequest);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
      @AuthenticationPrincipal CustomUser customUser) {

    ProfileResponse response = userCommandService.getProfile(customUser);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @PatchMapping("/profile")
  public ResponseEntity<ApiResponse<ProfileResponse>> patchProfile(
      @AuthenticationPrincipal CustomUser customUser,
      @RequestPart @Valid PatchProfileRequest profileRequest,
      @RequestPart(required = false) MultipartFile profile) {

    ProfileResponse response =
        userCommandService.patchProfile(customUser.getUserId(), profileRequest, profile);

    return ResponseEntity.ok().body(ApiResponse.success(response));
  }

  @PatchMapping("/password")
  public ResponseEntity<ApiResponse<Void>> patchPassword(
      @RequestBody @Valid PatchPasswordRequest passwordRequest) {

    userCommandService.patchPaassword(passwordRequest);

    return ResponseEntity.ok().body(ApiResponse.success(null));
  }
}
