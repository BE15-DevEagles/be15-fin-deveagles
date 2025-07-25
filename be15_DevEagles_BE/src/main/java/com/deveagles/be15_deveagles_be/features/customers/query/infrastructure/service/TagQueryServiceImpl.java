package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.service;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.application.dto.response.TagResponse;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Tag;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.TagRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.service.TagQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TagQueryServiceImpl implements TagQueryService {

  private final TagRepository tagRepository;

  @Override
  public TagResponse getTag(Long tagId, Long shopId) {
    log.info("태그 조회 요청 - 태그ID: {}, 매장ID: {}", tagId, shopId);

    Tag tag =
        tagRepository
            .findByIdAndShopId(tagId, shopId)
            .orElseThrow(
                () -> {
                  log.error("태그를 찾을 수 없음 - 태그ID: {}, 매장ID: {}", tagId, shopId);
                  return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "태그를 찾을 수 없습니다.");
                });

    TagResponse response = TagResponse.from(tag);
    log.info("태그 조회 완료 - 태그ID: {}", tagId);
    return response;
  }

  @Override
  public List<TagResponse> getAllTags() {
    log.info("전체 태그 목록 조회 요청");

    List<Tag> tags = tagRepository.findAll();
    List<TagResponse> responses = tags.stream().map(this::mapToResponse).toList();

    log.info("전체 태그 목록 조회 완료 - 총 {}개", responses.size());
    return responses;
  }

  @Override
  public List<TagResponse> getAllTagsByShopId(Long shopId) {
    log.info("매장별 태그 목록 조회 요청 - 매장ID: {}", shopId);

    List<Tag> tags = tagRepository.findByShopId(shopId);
    List<TagResponse> responses = tags.stream().map(this::mapToResponse).toList();

    log.info("매장별 태그 목록 조회 완료 - 매장ID: {}, 총 {}개", shopId, responses.size());
    return responses;
  }

  @Override
  public Page<TagResponse> getTags(Pageable pageable) {
    log.info("태그 페이징 조회 요청 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());

    Page<Tag> tags = tagRepository.findAll(pageable);
    Page<TagResponse> responses = tags.map(this::mapToResponse);

    log.info(
        "태그 페이징 조회 완료 - 현재페이지: {}, 전체페이지: {}, 전체개수: {}",
        responses.getNumber(),
        responses.getTotalPages(),
        responses.getTotalElements());

    return responses;
  }

  private TagResponse mapToResponse(Tag tag) {
    return TagResponse.builder()
        .tagId(tag.getId())
        .shopId(tag.getShopId())
        .tagName(tag.getTagName())
        .colorCode(tag.getColorCode())
        .build();
  }
}
