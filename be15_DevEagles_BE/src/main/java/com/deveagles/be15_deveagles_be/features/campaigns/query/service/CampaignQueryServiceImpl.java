package com.deveagles.be15_deveagles_be.features.campaigns.query.service;

import com.deveagles.be15_deveagles_be.common.dto.PagedResult;
import com.deveagles.be15_deveagles_be.features.campaigns.command.domain.aggregate.Campaign;
import com.deveagles.be15_deveagles_be.features.campaigns.query.dto.request.CampaignSearchRequest;
import com.deveagles.be15_deveagles_be.features.campaigns.query.dto.response.CampaignQueryResponse;
import com.deveagles.be15_deveagles_be.features.campaigns.query.repository.CampaignQueryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignQueryServiceImpl implements CampaignQueryService {

  private final CampaignQueryRepository campaignQueryRepository;

  @Override
  public PagedResult<CampaignQueryResponse> getCampaignsByShop(CampaignSearchRequest request) {
    Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

    Page<Campaign> campaignPage =
        campaignQueryRepository.findByShopIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            request.getShopId(), pageable);

    Page<CampaignQueryResponse> responsePage =
        campaignPage.map(
            campaign -> {
              CampaignQueryResponse response = CampaignQueryResponse.from(campaign);

              return response;
            });

    return PagedResult.from(responsePage);
  }

  @Override
  public Optional<CampaignQueryResponse> getCampaignById(Long id) {
    Optional<Campaign> campaign = campaignQueryRepository.findByIdAndDeletedAtIsNull(id);

    return campaign.map(
        c -> {
          CampaignQueryResponse response = CampaignQueryResponse.from(c);
          return response;
        });
  }
}
