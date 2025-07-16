package com.deveagles.be15_deveagles_be.features.customers.query.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.SegmentCustomersResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SegmentCustomerQueryController.class)
@WithMockUser
@DisplayName("세그먼트별 고객 조회 컨트롤러 테스트")
class SegmentCustomerQueryControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CustomerQueryService customerQueryService;

  @Nested
  @DisplayName("세그먼트 태그별 고객 조회")
  class GetCustomersBySegmentTagTest {

    @Test
    @DisplayName("정상적인 세그먼트 태그로 고객 조회에 성공한다")
    void getCustomersBySegmentTag_Success() throws Exception {
      // given
      String segmentTag = "VIP";
      List<Long> customerIds = Arrays.asList(1L, 2L, 3L);
      SegmentCustomersResponse response = SegmentCustomersResponse.of("VIP", "VIP 고객", customerIds);

      given(customerQueryService.getCustomersBySegmentTag("VIP")).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/{segmentTag}/customers", segmentTag)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.segmentTag").value("VIP"))
          .andExpect(jsonPath("$.data.segmentTitle").value("VIP 고객"))
          .andExpect(jsonPath("$.data.customerCount").value(3))
          .andExpect(jsonPath("$.data.customerIds").isArray())
          .andExpect(jsonPath("$.data.customerIds[0]").value(1))
          .andExpect(jsonPath("$.data.customerIds[1]").value(2))
          .andExpect(jsonPath("$.data.customerIds[2]").value(3))
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentTag("VIP");
    }

    @Test
    @DisplayName("소문자 태그도 대문자로 변환하여 조회한다")
    void getCustomersBySegmentTag_LowerCaseTag() throws Exception {
      // given
      String segmentTag = "loyal";
      List<Long> customerIds = Arrays.asList(10L, 20L);
      SegmentCustomersResponse response =
          SegmentCustomersResponse.of("LOYAL", "충성 고객", customerIds);

      given(customerQueryService.getCustomersBySegmentTag("LOYAL")).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/{segmentTag}/customers", segmentTag)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.segmentTag").value("LOYAL"))
          .andExpect(jsonPath("$.data.segmentTitle").value("충성 고객"))
          .andExpect(jsonPath("$.data.customerCount").value(2))
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentTag("LOYAL");
    }

    @Test
    @DisplayName("존재하지 않는 세그먼트 태그로 조회 시 404 응답을 반환한다")
    void getCustomersBySegmentTag_NotFound() throws Exception {
      // given
      String segmentTag = "INVALID";
      given(customerQueryService.getCustomersBySegmentTag("INVALID"))
          .willThrow(new BusinessException(ErrorCode.SEGMENT_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              get("/api/segments/{segmentTag}/customers", segmentTag)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("30007"))
          .andExpect(jsonPath("$.message").value("세그먼트를 찾을 수 없습니다"))
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentTag("INVALID");
    }

    @Test
    @DisplayName("빈 고객 목록을 가진 세그먼트 조회에 성공한다")
    void getCustomersBySegmentTag_EmptyCustomers() throws Exception {
      // given
      String segmentTag = "NEW";
      SegmentCustomersResponse response =
          SegmentCustomersResponse.of("NEW", "신규 고객", Collections.emptyList());

      given(customerQueryService.getCustomersBySegmentTag("NEW")).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/{segmentTag}/customers", segmentTag)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.segmentTag").value("NEW"))
          .andExpect(jsonPath("$.data.segmentTitle").value("신규 고객"))
          .andExpect(jsonPath("$.data.customerCount").value(0))
          .andExpect(jsonPath("$.data.customerIds").isEmpty())
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentTag("NEW");
    }
  }

  @Nested
  @DisplayName("세그먼트 ID별 고객 조회")
  class GetCustomersBySegmentIdTest {

    @Test
    @DisplayName("정상적인 세그먼트 ID로 고객 조회에 성공한다")
    void getCustomersBySegmentId_Success() throws Exception {
      // given
      Long segmentId = 1L;
      List<Long> customerIds = Arrays.asList(100L, 200L);
      SegmentCustomersResponse response = SegmentCustomersResponse.of("NEW", "신규 고객", customerIds);

      given(customerQueryService.getCustomersBySegmentId(segmentId)).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/id/{segmentId}/customers", segmentId)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.segmentTag").value("NEW"))
          .andExpect(jsonPath("$.data.segmentTitle").value("신규 고객"))
          .andExpect(jsonPath("$.data.customerCount").value(2))
          .andExpect(jsonPath("$.data.customerIds[0]").value(100))
          .andExpect(jsonPath("$.data.customerIds[1]").value(200))
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentId(segmentId);
    }

    @Test
    @DisplayName("존재하지 않는 세그먼트 ID로 조회 시 404 응답을 반환한다")
    void getCustomersBySegmentId_NotFound() throws Exception {
      // given
      Long segmentId = 999L;
      given(customerQueryService.getCustomersBySegmentId(segmentId))
          .willThrow(new BusinessException(ErrorCode.SEGMENT_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              get("/api/segments/id/{segmentId}/customers", segmentId)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errorCode").value("30007"))
          .andExpect(jsonPath("$.message").value("세그먼트를 찾을 수 없습니다"))
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentId(segmentId);
    }
  }

  @Nested
  @DisplayName("다중 세그먼트별 고객 조회")
  class GetCustomersByMultipleSegmentTagsTest {

    @Test
    @DisplayName("여러 세그먼트 태그로 고객 조회에 성공한다")
    void getCustomersByMultipleSegmentTags_Success() throws Exception {
      // given
      List<String> segmentTags = Arrays.asList("NEW", "GROWING", "LOYAL");
      List<SegmentCustomersResponse> responses =
          Arrays.asList(
              SegmentCustomersResponse.of("NEW", "신규 고객", Arrays.asList(1L, 2L)),
              SegmentCustomersResponse.of("GROWING", "성장 고객", Arrays.asList(3L, 4L, 5L)),
              SegmentCustomersResponse.of("LOYAL", "충성 고객", Arrays.asList(6L, 7L, 8L, 9L)));

      given(
              customerQueryService.getCustomersByMultipleSegmentTags(
                  Arrays.asList("NEW", "GROWING", "LOYAL")))
          .willReturn(responses);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/customers")
                  .param("segmentTags", "NEW,GROWING,LOYAL")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(3))
          .andExpect(jsonPath("$.data[0].segmentTag").value("NEW"))
          .andExpect(jsonPath("$.data[0].customerCount").value(2))
          .andExpect(jsonPath("$.data[1].segmentTag").value("GROWING"))
          .andExpect(jsonPath("$.data[1].customerCount").value(3))
          .andExpect(jsonPath("$.data[2].segmentTag").value("LOYAL"))
          .andExpect(jsonPath("$.data[2].customerCount").value(4))
          .andDo(print());

      verify(customerQueryService)
          .getCustomersByMultipleSegmentTags(Arrays.asList("NEW", "GROWING", "LOYAL"));
    }

    @Test
    @DisplayName("소문자 태그들도 대문자로 변환하여 조회한다")
    void getCustomersByMultipleSegmentTags_LowerCaseTags() throws Exception {
      // given
      List<SegmentCustomersResponse> responses =
          Arrays.asList(
              SegmentCustomersResponse.of("VIP", "VIP 고객", Arrays.asList(1L)),
              SegmentCustomersResponse.of("DORMANT", "휴면 고객", Arrays.asList(2L, 3L)));

      given(customerQueryService.getCustomersByMultipleSegmentTags(Arrays.asList("VIP", "DORMANT")))
          .willReturn(responses);

      // when & then
      mockMvc
          .perform(
              get("/api/segments/customers")
                  .param("segmentTags", "vip,dormant")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[0].segmentTag").value("VIP"))
          .andExpect(jsonPath("$.data[1].segmentTag").value("DORMANT"))
          .andDo(print());

      verify(customerQueryService)
          .getCustomersByMultipleSegmentTags(Arrays.asList("VIP", "DORMANT"));
    }

    @Test
    @DisplayName("빈 세그먼트 태그 목록으로 조회 시 빈 배열을 반환한다")
    void getCustomersByMultipleSegmentTags_EmptyList() throws Exception {
      // given
      given(customerQueryService.getCustomersByMultipleSegmentTags(Collections.emptyList()))
          .willReturn(Collections.emptyList());

      // when & then
      mockMvc
          .perform(
              get("/api/segments/customers")
                  .param("segmentTags", "")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(0))
          .andDo(print());

      verify(customerQueryService).getCustomersByMultipleSegmentTags(Collections.emptyList());
    }
  }

  @Nested
  @DisplayName("라이프사이클 세그먼트별 고객 조회")
  class GetCustomersByLifecycleSegmentsTest {

    @Test
    @DisplayName("라이프사이클 세그먼트 전체 조회에 성공한다")
    void getCustomersByLifecycleSegments_Success() throws Exception {
      // given
      List<String> lifecycleSegmentTags =
          Arrays.asList(
              "NEW",
              "GROWING",
              "LOYAL",
              "VIP",
              "DORMANT",
              "NEW_FOLLOWUP",
              "NEW_AT_RISK",
              "REACTIVATION_NEEDED",
              "GROWING_DELAYED",
              "LOYAL_DELAYED");

      List<SegmentCustomersResponse> responses =
          Arrays.asList(
              SegmentCustomersResponse.of("NEW", "신규 고객", Arrays.asList(1L, 2L)),
              SegmentCustomersResponse.of("GROWING", "성장 고객", Arrays.asList(3L, 4L)),
              SegmentCustomersResponse.of("LOYAL", "충성 고객", Arrays.asList(5L, 6L)),
              SegmentCustomersResponse.of("VIP", "VIP 고객", Arrays.asList(7L)),
              SegmentCustomersResponse.of("DORMANT", "휴면 고객", Arrays.asList(8L, 9L)),
              SegmentCustomersResponse.of("NEW_FOLLOWUP", "신규 고객 팔로업 필요", Arrays.asList(10L)),
              SegmentCustomersResponse.of("NEW_AT_RISK", "신규 고객 이탈 위험", Arrays.asList(11L)),
              SegmentCustomersResponse.of("REACTIVATION_NEEDED", "재활성화 필요", Arrays.asList(12L)),
              SegmentCustomersResponse.of("GROWING_DELAYED", "성장 고객 방문 지연", Arrays.asList(13L)),
              SegmentCustomersResponse.of("LOYAL_DELAYED", "충성 고객 방문 지연", Arrays.asList(14L)));

      given(customerQueryService.getCustomersByMultipleSegmentTags(lifecycleSegmentTags))
          .willReturn(responses);

      // when & then
      mockMvc
          .perform(get("/api/segments/lifecycle/customers").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(10))
          .andExpect(jsonPath("$.data[0].segmentTag").value("NEW"))
          .andExpect(jsonPath("$.data[1].segmentTag").value("GROWING"))
          .andExpect(jsonPath("$.data[2].segmentTag").value("LOYAL"))
          .andExpect(jsonPath("$.data[3].segmentTag").value("VIP"))
          .andExpect(jsonPath("$.data[4].segmentTag").value("DORMANT"))
          .andExpect(jsonPath("$.data[5].segmentTag").value("NEW_FOLLOWUP"))
          .andExpect(jsonPath("$.data[6].segmentTag").value("NEW_AT_RISK"))
          .andExpect(jsonPath("$.data[7].segmentTag").value("REACTIVATION_NEEDED"))
          .andExpect(jsonPath("$.data[8].segmentTag").value("GROWING_DELAYED"))
          .andExpect(jsonPath("$.data[9].segmentTag").value("LOYAL_DELAYED"))
          .andDo(print());

      verify(customerQueryService).getCustomersByMultipleSegmentTags(lifecycleSegmentTags);
    }

    @Test
    @DisplayName("빈 라이프사이클 세그먼트 목록도 정상 처리한다")
    void getCustomersByLifecycleSegments_EmptyResults() throws Exception {
      // given
      List<String> lifecycleSegmentTags =
          Arrays.asList(
              "NEW",
              "GROWING",
              "LOYAL",
              "VIP",
              "DORMANT",
              "NEW_FOLLOWUP",
              "NEW_AT_RISK",
              "REACTIVATION_NEEDED",
              "GROWING_DELAYED",
              "LOYAL_DELAYED");

      given(customerQueryService.getCustomersByMultipleSegmentTags(lifecycleSegmentTags))
          .willReturn(Collections.emptyList());

      // when & then
      mockMvc
          .perform(get("/api/segments/lifecycle/customers").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(0))
          .andDo(print());

      verify(customerQueryService).getCustomersByMultipleSegmentTags(lifecycleSegmentTags);
    }
  }

  @Nested
  @DisplayName("예외 상황 테스트")
  class ExceptionTest {

    @Test
    @DisplayName("서비스 레이어 예외가 발생하면 적절한 에러 응답을 반환한다")
    void handleServiceException() throws Exception {
      // given
      given(customerQueryService.getCustomersBySegmentTag(anyString()))
          .willThrow(new RuntimeException("Database connection failed"));

      // when & then
      mockMvc
          .perform(get("/api/segments/NEW/customers").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError())
          .andDo(print());

      verify(customerQueryService).getCustomersBySegmentTag("NEW");
    }

    @Test
    @DisplayName("잘못된 경로 파라미터 요청 시 적절한 에러 응답을 반환한다")
    void handleBadRequest() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/segments/id/invalid/customers").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andDo(print());
    }
  }
}
