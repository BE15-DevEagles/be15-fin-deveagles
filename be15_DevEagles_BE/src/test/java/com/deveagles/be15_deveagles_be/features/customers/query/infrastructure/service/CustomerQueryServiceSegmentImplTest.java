package com.deveagles.be15_deveagles_be.features.customers.query.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Segment;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentByCustomerRepository;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.SegmentRepository;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.SegmentCustomersResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerQueryService 세그먼트 관련 메서드 테스트")
class CustomerQueryServiceSegmentImplTest {

  @Mock private SegmentRepository segmentRepository;

  @Mock private SegmentByCustomerRepository segmentByCustomerRepository;

  @InjectMocks private CustomerQueryServiceImpl customerQueryService;

  private Segment vipSegment;
  private Segment newSegment;
  private Segment loyalSegment;

  @BeforeEach
  void setUp() {
    vipSegment = createSegment(1L, "VIP", "VIP 고객");
    newSegment = createSegment(2L, "NEW", "신규 고객");
    loyalSegment = createSegment(3L, "LOYAL", "충성 고객");
  }

  private Segment createSegment(Long id, String tag, String title) {
    return Segment.builder()
        .id(id)
        .segmentTag(tag)
        .segmentTitle(title)
        .colorCode("#00BFFF")
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  @Nested
  @DisplayName("세그먼트 태그별 고객 조회 테스트")
  class GetCustomersBySegmentTagTest {

    @Test
    @DisplayName("정상적인 세그먼트 태그로 고객 조회에 성공한다")
    void getCustomersBySegmentTag_Success() {
      // given
      String segmentTag = "VIP";
      List<Long> customerIds = Arrays.asList(1L, 2L, 3L);

      given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.of(vipSegment));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag(segmentTag))
          .willReturn(customerIds);

      // when
      SegmentCustomersResponse result = customerQueryService.getCustomersBySegmentTag(segmentTag);

      // then
      assertThat(result.segmentTag()).isEqualTo("VIP");
      assertThat(result.segmentTitle()).isEqualTo("VIP 고객");
      assertThat(result.customerCount()).isEqualTo(3);
      assertThat(result.customerIds()).containsExactly(1L, 2L, 3L);

      verify(segmentRepository).findBySegmentTag(segmentTag);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag(segmentTag);
    }

    @Test
    @DisplayName("존재하지 않는 세그먼트 태그로 조회 시 예외가 발생한다")
    void getCustomersBySegmentTag_NotFound() {
      // given
      String segmentTag = "INVALID";
      given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentTag(segmentTag))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("세그먼트를 찾을 수 없습니다: INVALID");

      verify(segmentRepository).findBySegmentTag(segmentTag);
    }

    @Test
    @DisplayName("빈 고객 목록을 가진 세그먼트 조회에 성공한다")
    void getCustomersBySegmentTag_EmptyCustomers() {
      // given
      String segmentTag = "NEW";
      List<Long> customerIds = Collections.emptyList();

      given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.of(newSegment));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag(segmentTag))
          .willReturn(customerIds);

      // when
      SegmentCustomersResponse result = customerQueryService.getCustomersBySegmentTag(segmentTag);

      // then
      assertThat(result.segmentTag()).isEqualTo("NEW");
      assertThat(result.segmentTitle()).isEqualTo("신규 고객");
      assertThat(result.customerCount()).isEqualTo(0);
      assertThat(result.customerIds()).isEmpty();

      verify(segmentRepository).findBySegmentTag(segmentTag);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag(segmentTag);
    }
  }

  @Nested
  @DisplayName("세그먼트 ID별 고객 조회 테스트")
  class GetCustomersBySegmentIdTest {

    @Test
    @DisplayName("정상적인 세그먼트 ID로 고객 조회에 성공한다")
    void getCustomersBySegmentId_Success() {
      // given
      Long segmentId = 1L;
      List<Long> customerIds = Arrays.asList(10L, 20L, 30L);

      given(segmentRepository.findById(segmentId)).willReturn(Optional.of(vipSegment));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentId(segmentId))
          .willReturn(customerIds);

      // when
      SegmentCustomersResponse result = customerQueryService.getCustomersBySegmentId(segmentId);

      // then
      assertThat(result.segmentTag()).isEqualTo("VIP");
      assertThat(result.segmentTitle()).isEqualTo("VIP 고객");
      assertThat(result.customerCount()).isEqualTo(3);
      assertThat(result.customerIds()).containsExactly(10L, 20L, 30L);

      verify(segmentRepository).findById(segmentId);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentId(segmentId);
    }

    @Test
    @DisplayName("존재하지 않는 세그먼트 ID로 조회 시 예외가 발생한다")
    void getCustomersBySegmentId_NotFound() {
      // given
      Long segmentId = 999L;
      given(segmentRepository.findById(segmentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentId(segmentId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("세그먼트를 찾을 수 없습니다: 999");

      verify(segmentRepository).findById(segmentId);
    }

    @Test
    @DisplayName("빈 고객 목록을 가진 세그먼트 ID 조회에 성공한다")
    void getCustomersBySegmentId_EmptyCustomers() {
      // given
      Long segmentId = 2L;
      List<Long> customerIds = Collections.emptyList();

      given(segmentRepository.findById(segmentId)).willReturn(Optional.of(newSegment));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentId(segmentId))
          .willReturn(customerIds);

      // when
      SegmentCustomersResponse result = customerQueryService.getCustomersBySegmentId(segmentId);

      // then
      assertThat(result.segmentTag()).isEqualTo("NEW");
      assertThat(result.segmentTitle()).isEqualTo("신규 고객");
      assertThat(result.customerCount()).isEqualTo(0);
      assertThat(result.customerIds()).isEmpty();

      verify(segmentRepository).findById(segmentId);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentId(segmentId);
    }
  }

  @Nested
  @DisplayName("다중 세그먼트 태그별 고객 조회 테스트")
  class GetCustomersByMultipleSegmentTagsTest {

    @Test
    @DisplayName("여러 세그먼트 태그로 고객 조회에 성공한다")
    void getCustomersByMultipleSegmentTags_Success() {
      // given
      List<String> segmentTags = Arrays.asList("VIP", "NEW", "LOYAL");
      List<Segment> segments = Arrays.asList(vipSegment, newSegment, loyalSegment);

      given(segmentRepository.findBySegmentTagIn(segmentTags)).willReturn(segments);
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("VIP"))
          .willReturn(Arrays.asList(1L, 2L));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("NEW"))
          .willReturn(Arrays.asList(3L, 4L, 5L));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("LOYAL"))
          .willReturn(Arrays.asList(6L));

      // when
      List<SegmentCustomersResponse> results =
          customerQueryService.getCustomersByMultipleSegmentTags(segmentTags);

      // then
      assertThat(results).hasSize(3);

      assertThat(results.get(0).segmentTag()).isEqualTo("VIP");
      assertThat(results.get(0).segmentTitle()).isEqualTo("VIP 고객");
      assertThat(results.get(0).customerCount()).isEqualTo(2);
      assertThat(results.get(0).customerIds()).containsExactly(1L, 2L);

      assertThat(results.get(1).segmentTag()).isEqualTo("NEW");
      assertThat(results.get(1).segmentTitle()).isEqualTo("신규 고객");
      assertThat(results.get(1).customerCount()).isEqualTo(3);
      assertThat(results.get(1).customerIds()).containsExactly(3L, 4L, 5L);

      assertThat(results.get(2).segmentTag()).isEqualTo("LOYAL");
      assertThat(results.get(2).segmentTitle()).isEqualTo("충성 고객");
      assertThat(results.get(2).customerCount()).isEqualTo(1);
      assertThat(results.get(2).customerIds()).containsExactly(6L);

      verify(segmentRepository).findBySegmentTagIn(segmentTags);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("VIP");
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("NEW");
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("LOYAL");
    }

    @Test
    @DisplayName("존재하지 않는 세그먼트 태그가 포함된 경우 Unknown Segment로 표시한다")
    void getCustomersByMultipleSegmentTags_WithUnknownSegment() {
      // given
      List<String> segmentTags = Arrays.asList("VIP", "UNKNOWN", "NEW");
      List<Segment> segments = Arrays.asList(vipSegment, newSegment); // UNKNOWN 세그먼트는 없음

      given(segmentRepository.findBySegmentTagIn(segmentTags)).willReturn(segments);
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("VIP"))
          .willReturn(Arrays.asList(1L, 2L));
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("NEW"))
          .willReturn(Arrays.asList(3L, 4L));

      // when
      List<SegmentCustomersResponse> results =
          customerQueryService.getCustomersByMultipleSegmentTags(segmentTags);

      // then
      assertThat(results).hasSize(3);

      assertThat(results.get(0).segmentTag()).isEqualTo("VIP");
      assertThat(results.get(0).segmentTitle()).isEqualTo("VIP 고객");
      assertThat(results.get(0).customerCount()).isEqualTo(2);

      assertThat(results.get(1).segmentTag()).isEqualTo("UNKNOWN");
      assertThat(results.get(1).segmentTitle()).isEqualTo("Unknown Segment");
      assertThat(results.get(1).customerCount()).isEqualTo(0);

      assertThat(results.get(2).segmentTag()).isEqualTo("NEW");
      assertThat(results.get(2).segmentTitle()).isEqualTo("신규 고객");
      assertThat(results.get(2).customerCount()).isEqualTo(2);

      verify(segmentRepository).findBySegmentTagIn(segmentTags);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("VIP");
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("NEW");
      // Note: UNKNOWN segment is not in the database, so findCustomerIdsBySegmentTag is not called
      // for it
    }

    @Test
    @DisplayName("빈 세그먼트 태그 목록으로 조회 시 빈 결과를 반환한다")
    void getCustomersByMultipleSegmentTags_EmptyList() {
      // given
      List<String> segmentTags = Collections.emptyList();

      // when
      List<SegmentCustomersResponse> results =
          customerQueryService.getCustomersByMultipleSegmentTags(segmentTags);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("모든 세그먼트가 빈 고객 목록을 가져도 정상 처리한다")
    void getCustomersByMultipleSegmentTags_AllEmpty() {
      // given
      List<String> segmentTags = Arrays.asList("VIP", "NEW");
      List<Segment> segments = Arrays.asList(vipSegment, newSegment);

      given(segmentRepository.findBySegmentTagIn(segmentTags)).willReturn(segments);
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("VIP"))
          .willReturn(Collections.emptyList());
      given(segmentByCustomerRepository.findCustomerIdsBySegmentTag("NEW"))
          .willReturn(Collections.emptyList());

      // when
      List<SegmentCustomersResponse> results =
          customerQueryService.getCustomersByMultipleSegmentTags(segmentTags);

      // then
      assertThat(results).hasSize(2);

      assertThat(results.get(0).segmentTag()).isEqualTo("VIP");
      assertThat(results.get(0).customerCount()).isEqualTo(0);
      assertThat(results.get(0).customerIds()).isEmpty();

      assertThat(results.get(1).segmentTag()).isEqualTo("NEW");
      assertThat(results.get(1).customerCount()).isEqualTo(0);
      assertThat(results.get(1).customerIds()).isEmpty();

      verify(segmentRepository).findBySegmentTagIn(segmentTags);
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("VIP");
      verify(segmentByCustomerRepository).findCustomerIdsBySegmentTag("NEW");
    }
  }

  @Nested
  @DisplayName("예외 처리 테스트")
  class ExceptionHandlingTest {

    @Test
    @DisplayName("세그먼트 태그별 조회 시 SEGMENT_NOT_FOUND 예외가 발생한다")
    void getCustomersBySegmentTag_ThrowsSegmentNotFound() {
      // given
      String segmentTag = "INVALID";
      given(segmentRepository.findBySegmentTag(segmentTag)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentTag(segmentTag))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SEGMENT_NOT_FOUND);

      verify(segmentRepository).findBySegmentTag(segmentTag);
    }

    @Test
    @DisplayName("세그먼트 ID별 조회 시 SEGMENT_NOT_FOUND 예외가 발생한다")
    void getCustomersBySegmentId_ThrowsSegmentNotFound() {
      // given
      Long segmentId = 999L;
      given(segmentRepository.findById(segmentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentId(segmentId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SEGMENT_NOT_FOUND);

      verify(segmentRepository).findById(segmentId);
    }

    @Test
    @DisplayName("Repository 예외 발생 시 예외가 전파된다")
    void repositoryException_Propagated() {
      // given
      String segmentTag = "VIP";
      given(segmentRepository.findBySegmentTag(segmentTag))
          .willThrow(new RuntimeException("Database connection failed"));

      // when & then
      assertThatThrownBy(() -> customerQueryService.getCustomersBySegmentTag(segmentTag))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Database connection failed");

      verify(segmentRepository).findBySegmentTag(segmentTag);
    }
  }
}
