package com.deveagles.be15_deveagles_be.features.campaigns.query.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.campaigns.command.domain.aggregate.Campaign;
import com.deveagles.be15_deveagles_be.features.campaigns.query.dto.request.CampaignSearchRequest;
import com.deveagles.be15_deveagles_be.features.campaigns.query.dto.response.CampaignQueryResponse;
import com.deveagles.be15_deveagles_be.features.campaigns.query.repository.CampaignQueryRepository;
import com.deveagles.be15_deveagles_be.features.campaigns.query.service.CampaignQueryServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignQueryService 단위 테스트")
class CampaignQueryServiceImplTest {

  @Mock private CampaignQueryRepository campaignQueryRepository;

  @InjectMocks private CampaignQueryServiceImpl campaignQueryService;

  @Test
  @DisplayName("매장 ID로 캠페인 목록 조회 성공")
  void getCampaignsByShop_매장ID로캠페인목록조회_성공() {
    // Given
    Long shopId = 1L;
    CampaignSearchRequest request = new CampaignSearchRequest(shopId, 0, 10);

    Campaign campaign1 =
        Campaign.builder()
            .id(1L)
            .campaignTitle("첫 번째 캠페인")
            .description("첫 번째 캠페인 설명")
            .startDate(LocalDate.now().minusDays(10))
            .endDate(LocalDate.now().plusDays(10))
            .messageSendAt(LocalDateTime.now().minusDays(5))
            .staffId(1L)
            .templateId(1L)
            .couponId(1L)
            .shopId(shopId)
            .createdAt(LocalDateTime.now().minusDays(15))
            .build();

    Campaign campaign2 =
        Campaign.builder()
            .id(2L)
            .campaignTitle("두 번째 캠페인")
            .description("두 번째 캠페인 설명")
            .startDate(LocalDate.now().minusDays(20))
            .endDate(LocalDate.now().plusDays(20))
            .messageSendAt(LocalDateTime.now().minusDays(10))
            .staffId(1L)
            .templateId(2L)
            .couponId(2L)
            .shopId(shopId)
            .createdAt(LocalDateTime.now().minusDays(25))
            .build();

    List<Campaign> campaigns = Arrays.asList(campaign1, campaign2);
    Page<Campaign> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), 2);

    given(
            campaignQueryRepository.findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(shopId), any(Pageable.class)))
        .willReturn(campaignPage);

    // When
    PagedResult<CampaignQueryResponse> result = campaignQueryService.getCampaignsByShop(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(2);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(1);
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(0);

    // 첫 번째 캠페인 검증
    CampaignQueryResponse firstResponse = result.getContent().get(0);
    assertThat(firstResponse.getId()).isEqualTo(1L);
    assertThat(firstResponse.getCampaignTitle()).isEqualTo("첫 번째 캠페인");
    assertThat(firstResponse.getDescription()).isEqualTo("첫 번째 캠페인 설명");
    assertThat(firstResponse.getShopId()).isEqualTo(shopId);
    assertThat(firstResponse.isActive()).isTrue(); // 현재 날짜 기준으로 활성 상태

    // 두 번째 캠페인 검증
    CampaignQueryResponse secondResponse = result.getContent().get(1);
    assertThat(secondResponse.getId()).isEqualTo(2L);
    assertThat(secondResponse.getCampaignTitle()).isEqualTo("두 번째 캠페인");
    assertThat(secondResponse.getDescription()).isEqualTo("두 번째 캠페인 설명");
    assertThat(secondResponse.getShopId()).isEqualTo(shopId);

    then(campaignQueryRepository)
        .should(times(1))
        .findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(shopId), any(Pageable.class));
  }

  @Test
  @DisplayName("존재하지 않는 매장 ID로 조회시 빈 페이지 반환")
  void getCampaignsByShop_존재하지않는매장ID로조회_빈페이지반환() {
    // Given
    Long nonExistentShopId = 999L;
    CampaignSearchRequest request = new CampaignSearchRequest(nonExistentShopId, 0, 10);

    Page<Campaign> emptyCampaignPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    given(
            campaignQueryRepository.findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(nonExistentShopId), any(Pageable.class)))
        .willReturn(emptyCampaignPage);

    // When
    PagedResult<CampaignQueryResponse> result = campaignQueryService.getCampaignsByShop(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getPagination().getTotalItems()).isEqualTo(0);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(0);
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(0);

    then(campaignQueryRepository)
        .should(times(1))
        .findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            eq(nonExistentShopId), any(Pageable.class));
  }

  @Test
  @DisplayName("페이징 파라미터 적용 성공")
  void getCampaignsByShop_페이징파라미터적용_성공() {
    // Given
    Long shopId = 1L;
    int page = 1;
    int size = 5;
    CampaignSearchRequest request = new CampaignSearchRequest(shopId, page, size);

    Campaign campaign =
        Campaign.builder()
            .id(1L)
            .campaignTitle("페이징 테스트 캠페인")
            .description("페이징 테스트 설명")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(5))
            .staffId(1L)
            .templateId(1L)
            .couponId(1L)
            .shopId(shopId)
            .createdAt(LocalDateTime.now())
            .build();

    List<Campaign> campaigns = List.of(campaign);
    Page<Campaign> campaignPage = new PageImpl<>(campaigns, PageRequest.of(page, size), 10);

    given(
            campaignQueryRepository.findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(shopId), any(Pageable.class)))
        .willReturn(campaignPage);

    // When
    PagedResult<CampaignQueryResponse> result = campaignQueryService.getCampaignsByShop(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(10);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(2);
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(page);

    then(campaignQueryRepository)
        .should(times(1))
        .findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(shopId), any(Pageable.class));
  }

  @Test
  @DisplayName("삭제된 캠페인 조회 제외")
  void getCampaignsByShop_삭제된캠페인조회_제외() {
    // Given
    Long shopId = 1L;
    CampaignSearchRequest request = new CampaignSearchRequest(shopId, 0, 10);

    // 삭제되지 않은 캠페인만 조회되도록 Mock 설정
    Campaign activeCampaign =
        Campaign.builder()
            .id(1L)
            .campaignTitle("활성 캠페인")
            .description("활성 캠페인 설명")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(5))
            .staffId(1L)
            .templateId(1L)
            .couponId(1L)
            .shopId(shopId)
            .createdAt(LocalDateTime.now())
            .build();

    List<Campaign> campaigns = List.of(activeCampaign);
    Page<Campaign> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), 1);

    given(
            campaignQueryRepository.findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(shopId), any(Pageable.class)))
        .willReturn(campaignPage);

    // When
    PagedResult<CampaignQueryResponse> result = campaignQueryService.getCampaignsByShop(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getCampaignTitle()).isEqualTo("활성 캠페인");

    then(campaignQueryRepository)
        .should(times(1))
        .findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(shopId), any(Pageable.class));
  }

  @Test
  @DisplayName("캠페인 ID로 상세 조회 성공")
  void getCampaignById_캠페인ID로상세조회_성공() {
    // Given
    Long campaignId = 1L;
    Campaign campaign =
        Campaign.builder()
            .id(campaignId)
            .campaignTitle("테스트 캠페인")
            .description("테스트 캠페인 설명")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(5))
            .messageSendAt(LocalDateTime.now().minusDays(1))
            .staffId(1L)
            .templateId(1L)
            .couponId(1L)
            .shopId(1L)
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    given(campaignQueryRepository.findByIdAndDeletedAtIsNull(campaignId))
        .willReturn(Optional.of(campaign));

    // When
    Optional<CampaignQueryResponse> result = campaignQueryService.getCampaignById(campaignId);

    // Then
    assertThat(result).isPresent();
    CampaignQueryResponse response = result.get();
    assertThat(response.getId()).isEqualTo(campaignId);
    assertThat(response.getCampaignTitle()).isEqualTo("테스트 캠페인");
    assertThat(response.getDescription()).isEqualTo("테스트 캠페인 설명");
    assertThat(response.getCouponId()).isEqualTo(1L);

    then(campaignQueryRepository).should(times(1)).findByIdAndDeletedAtIsNull(campaignId);
  }

  @Test
  @DisplayName("존재하지 않는 캠페인 ID로 조회시 빈 Optional 반환")
  void getCampaignById_존재하지않는캠페인ID로조회_빈Optional반환() {
    // Given
    Long nonExistentCampaignId = 999L;

    given(campaignQueryRepository.findByIdAndDeletedAtIsNull(nonExistentCampaignId))
        .willReturn(Optional.empty());

    // When
    Optional<CampaignQueryResponse> result =
        campaignQueryService.getCampaignById(nonExistentCampaignId);

    // Then
    assertThat(result).isEmpty();

    then(campaignQueryRepository)
        .should(times(1))
        .findByIdAndDeletedAtIsNull(nonExistentCampaignId);
  }

  @Test
  @DisplayName("쿠폰 ID가 없는 캠페인 조회시 쿠폰 링크 제외")
  void getCampaignById_쿠폰ID없는캠페인조회_쿠폰링크제외() {
    // Given
    Long campaignId = 1L;
    Campaign campaignWithoutCoupon =
        Campaign.builder()
            .id(campaignId)
            .campaignTitle("쿠폰 없는 캠페인")
            .description("쿠폰이 없는 캠페인")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(5))
            .staffId(1L)
            .templateId(1L)
            .couponId(null) // 쿠폰 ID 없음
            .shopId(1L)
            .createdAt(LocalDateTime.now())
            .build();

    given(campaignQueryRepository.findByIdAndDeletedAtIsNull(campaignId))
        .willReturn(Optional.of(campaignWithoutCoupon));

    // When
    Optional<CampaignQueryResponse> result = campaignQueryService.getCampaignById(campaignId);

    // Then
    assertThat(result).isPresent();
    CampaignQueryResponse response = result.get();

    then(campaignQueryRepository).should(times(1)).findByIdAndDeletedAtIsNull(campaignId);
  }
}
