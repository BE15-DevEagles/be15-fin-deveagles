package com.deveagles.be15_deveagles_be.features.users.command.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
import com.deveagles.be15_deveagles_be.features.users.command.domain.aggregate.Staff;
import com.deveagles.be15_deveagles_be.features.users.command.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffCommandService 단위 테스트")
public class StaffCommandServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private AccessListRepository accessListRepository;
  @Mock private AccessAuthRepository accessAuthRepository;
  @Mock private AccessAuthQueryRepository accessAuthQueryRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserCommandService userCommandService;
  @Mock private FileCommandService fileCommandService;
  @Mock private MultipartFile profile;

  private StaffCommandServiceImpl staffCommandService;

  @BeforeEach
  void setUp() {
    staffCommandService =
        new StaffCommandServiceImpl(
            userRepository,
            accessListRepository,
            accessAuthRepository,
            accessAuthQueryRepository,
            passwordEncoder,
            userCommandService,
            fileCommandService);
  }

  @Test
  @DisplayName("staffCreate: 프로필 없이 직원 생성 성공")
  void staffCreate_프로필없음_성공() {
    // given
    Long shopId = 1L;
    CreateStaffRequest request =
        new CreateStaffRequest(
            "홍길동", // staffName
            "staff01", // loginId
            "test@example.com", // email
            "1234", // password
            "01012345678", // phoneNumber
            "매니저" // grade
            );

    Mockito.when(passwordEncoder.encode("1234")).thenReturn("encodedPw");

    Staff savedStaff = Staff.builder().staffId(1L).loginId("staff01").build();

    Mockito.when(userRepository.save(Mockito.any())).thenReturn(savedStaff);
    Mockito.when(accessListRepository.findAll())
        .thenReturn(
            List.of(
                AccessList.builder().accessId(1L).build(),
                AccessList.builder().accessId(2L).build()));

    // when
    staffCommandService.staffCreate(shopId, request, null);

    // then
    verify(userRepository).save(Mockito.any(Staff.class));
    verify(accessAuthRepository, times(2)).save(Mockito.any(AccessAuth.class));
  }

  @Test
  @DisplayName("staffCreate: 프로필이 있을 때 직원 생성 성공")
  void staffCreate_프로필있음_성공() {
    // given
    Long shopId = 1L;
    CreateStaffRequest request =
        new CreateStaffRequest("이순신", "staff02", "lee@example.com", "5678", "01098765432", "부점장");

    Mockito.when(profile.isEmpty()).thenReturn(false);
    Mockito.when(userCommandService.saveProfile(profile))
        .thenReturn("https://s3.bucket/profile.jpg");
    Mockito.when(passwordEncoder.encode("5678")).thenReturn("encodedPw");

    Staff savedStaff = Staff.builder().staffId(2L).loginId("staff02").build();

    Mockito.when(userRepository.save(Mockito.any())).thenReturn(savedStaff);
    Mockito.when(accessListRepository.findAll())
        .thenReturn(List.of(AccessList.builder().accessId(1L).build()));

    // when
    staffCommandService.staffCreate(shopId, request, profile);

    // then
    verify(userCommandService).saveProfile(profile);
    verify(userRepository).save(Mockito.any(Staff.class));
    verify(accessAuthRepository).save(Mockito.any(AccessAuth.class));
  }

  @Test
  void getStaffDetail_정상_조회() {
    // given
    Long staffId = 1L;
    Staff staff =
        Staff.builder().staffId(staffId).staffName("홍길동").email("test@example.com").build();

    List<StaffPermissions> permissions =
        List.of(new StaffPermissions(1L, "예약", true, false, false, true));

    given(userRepository.findStaffByStaffId(staffId)).willReturn(Optional.of(staff));
    given(accessAuthQueryRepository.getAccessPermissionsByStaffId(staffId)).willReturn(permissions);

    // when
    StaffInfoResponse response = staffCommandService.getStaffDetail(staffId);

    // then
    assertThat(response.getStaffName()).isEqualTo("홍길동");
    assertThat(response.getPermissions()).hasSize(1);
  }

  @Test
  void getStaffDetail_존재하지_않는_직원() {
    // given
    given(userRepository.findStaffByStaffId(anyLong())).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> staffCommandService.getStaffDetail(999L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.STAFF_NOT_FOUND.getMessage());
  }

  @Test
  void putStaffDetail_정상_수정() {
    // given
    Long staffId = 1L;
    Staff staff =
        Staff.builder()
            .staffId(staffId)
            .staffName("기존이름")
            .email("old@example.com")
            .phoneNumber("010-0000-0000")
            .build();

    MultipartFile profile = mock(MultipartFile.class);
    given(profile.isEmpty()).willReturn(false);
    given(fileCommandService.saveProfile(profile)).willReturn("https://image.com/profile.jpg");

    PutStaffRequest request =
        new PutStaffRequest(
            "새이름",
            "new@example.com",
            "01012345678",
            "Manager",
            null,
            null,
            "새로운 직원입니다.",
            "#fffff",
            List.of(new PermissionItem(1L, true, true, false, false)));

    AccessAuth accessAuth = new AccessAuth(1L, staffId, true, false, false, false);
    given(userRepository.findStaffByStaffId(staffId)).willReturn(Optional.of(staff));
    given(accessAuthRepository.findAllByStaffId(staffId)).willReturn(List.of(accessAuth));

    // when
    staffCommandService.putStaffDetail(staffId, request, profile);

    // then
    verify(fileCommandService).saveProfile(profile);
    verify(userRepository).save(any(Staff.class));
    verify(accessAuthRepository).save(any(AccessAuth.class));
    assertThat(staff.getStaffName()).isEqualTo("새이름");
    assertThat(staff.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  void putStaffDetail_직원없음() {
    // given
    given(userRepository.findStaffByStaffId(anyLong())).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () -> staffCommandService.putStaffDetail(1L, mock(PutStaffRequest.class), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.STAFF_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("getStaffProfileUrl: 정상적으로 프로필 URL을 조회한다")
  void getStaffProfileUrl_정상() {
    // given
    Long staffId = 1L;
    Staff staff =
        Staff.builder().staffId(staffId).profileUrl("https://test-bucket.s3/profile1.png").build();

    given(userRepository.findStaffByStaffId(staffId)).willReturn(Optional.of(staff));

    // when
    var response = staffCommandService.getStaffProfileUrl(staffId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getProfileUrl()).isEqualTo("https://test-bucket.s3/profile1.png");
    verify(userRepository).findStaffByStaffId(staffId);
  }
}
