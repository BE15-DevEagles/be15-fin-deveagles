package com.deveagles.be15_deveagles_be.features.users.command.application.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.auth.command.domain.aggregate.AccessAuth;
import com.deveagles.be15_deveagles_be.features.auth.command.domain.aggregate.AccessList;
import com.deveagles.be15_deveagles_be.features.auth.command.repository.AccessAuthRepository;
import com.deveagles.be15_deveagles_be.features.auth.command.repository.AccessListRepository;
import com.deveagles.be15_deveagles_be.features.auth.query.infroStructure.repository.AccessAuthQueryRepository;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.CreateStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.PermissionItem;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.PutStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffInfoResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffPermissions;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffProfileUrlResponse;
import com.deveagles.be15_deveagles_be.features.users.command.domain.aggregate.Staff;
import com.deveagles.be15_deveagles_be.features.users.command.domain.aggregate.StaffStatus;
import com.deveagles.be15_deveagles_be.features.users.command.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffCommandServiceImpl implements StaffCommandService {

  private final UserRepository userRepository;
  private final AccessListRepository accessListRepository;
  private final AccessAuthRepository accessAuthRepository;
  private final AccessAuthQueryRepository accessAuthQueryRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserCommandService userCommandService;
  private final FileCommandService fileCommandService;

  @Override
  @Transactional
  public void staffCreate(Long shopId, CreateStaffRequest request, MultipartFile profile) {

    String profileUrl = null;

    if (profile != null && !profile.isEmpty()) {
      profileUrl = userCommandService.saveProfile(profile);
    }

    Staff staff =
        Staff.builder()
            .loginId(request.loginId())
            .password(passwordEncoder.encode(request.password()))
            .staffName(request.staffName())
            .grade(request.grade())
            .email(request.email())
            .shopId(shopId)
            .phoneNumber(request.phoneNumber())
            .colorCode("#364f6b")
            .staffStatus(StaffStatus.STAFF)
            .profileUrl(profileUrl)
            .build();

    Staff newStaff = userRepository.save(staff);

    List<AccessList> accessList = accessListRepository.findAll();

    for (AccessList access : accessList) {
      AccessAuth auth =
          AccessAuth.builder()
              .staffId(newStaff.getStaffId())
              .accessId(access.getAccessId())
              .canRead(true)
              .canWrite(false)
              .canDelete(false)
              .isActive(true)
              .build();

      accessAuthRepository.save(auth);
    }
  }

  @Override
  public StaffInfoResponse getStaffDetail(Long staffId) {

    Staff staff =
        userRepository
            .findStaffByStaffId(staffId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_FOUND));

    List<StaffPermissions> permissions =
        accessAuthQueryRepository.getAccessPermissionsByStaffId(staffId);

    return buildStaffInfoResponse(staff, permissions);
  }

  @Override
  public void putStaffDetail(Long staffId, PutStaffRequest request, MultipartFile profile) {

    Staff findStaff =
        userRepository
            .findStaffByStaffId(staffId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_FOUND));

    if (profile != null) {
      if (!profile.isEmpty()) {
        String profileUrl = fileCommandService.saveProfile(profile);
        findStaff.patchProfileUrl(profileUrl);
      } else {
        findStaff.patchProfileUrl(null);
      }
    }

    if (request.staffName() != null && !request.staffName().equals(findStaff.getStaffName())) {
      findStaff.patchName(request.staffName());
    }
    if (request.email() != null && !request.email().equals(findStaff.getEmail())) {
      findStaff.patchEmail(request.email());
    }
    if (request.phoneNumber() != null
        && !request.phoneNumber().equals(findStaff.getPhoneNumber())) {
      findStaff.patchPhoneNumber(request.phoneNumber());
    }
    if (request.grade() != null && !request.grade().equals(findStaff.getGrade())) {
      findStaff.patchGrade(request.grade());
    }
    if (request.description() != null
        && !request.description().equals(findStaff.getStaffDescription())) {
      findStaff.patchDescription(request.description());
    }
    if (request.colorCode() != null && !request.colorCode().equals(findStaff.getColorCode())) {
      findStaff.patchColorCode(request.colorCode());
    }

    if (request.joinedDate() != null) findStaff.patchJoinedDate(request.joinedDate());
    findStaff.patchLeftDate(request.leftDate());

    userRepository.save(findStaff);

    List<AccessAuth> originalPermissions = accessAuthRepository.findAllByStaffId(staffId);
    // 수정본
    Map<Long, PermissionItem> permissions =
        request.permissions().stream()
            .collect(Collectors.toMap(PermissionItem::accessId, Function.identity()));

    for (AccessAuth auth : originalPermissions) {
      PermissionItem perm = permissions.get(auth.getAccessId());
      if (perm != null) {
        auth.updateAccess(perm.active(), perm.canRead(), perm.canWrite(), perm.canDelete());
      }
      accessAuthRepository.save(auth);
    }
  }

  private StaffInfoResponse buildStaffInfoResponse(
      Staff staff, List<StaffPermissions> permissions) {

    boolean isWorking = staff.getLeftDate() == null;

    return StaffInfoResponse.builder()
        .staffName(staff.getStaffName())
        .email(staff.getEmail())
        .grade(staff.getGrade())
        .colorCode(staff.getColorCode())
        .isWorking(isWorking)
        .joinedDate(staff.getJoinedDate())
        .leftDate(staff.getLeftDate())
        .phoneNumber(staff.getPhoneNumber())
        .description(staff.getStaffDescription())
        .profileUrl(staff.getProfileUrl())
        .permissions(permissions)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public StaffProfileUrlResponse getStaffProfileUrl(Long staffId) {
    Staff staff =
        userRepository
            .findStaffByStaffId(staffId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_FOUND));

    return new StaffProfileUrlResponse(staff.getProfileUrl());
  }
}
