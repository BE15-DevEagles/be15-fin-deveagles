package com.deveagles.be15_deveagles_be.features.items.command.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.items.command.application.dto.request.SecondaryItemRegistRequest;
import com.deveagles.be15_deveagles_be.features.items.command.application.dto.request.SecondaryItemUpdateRequest;
import com.deveagles.be15_deveagles_be.features.items.command.domain.aggregate.Category;
import com.deveagles.be15_deveagles_be.features.items.command.domain.aggregate.PrimaryItem;
import com.deveagles.be15_deveagles_be.features.items.command.domain.aggregate.SecondaryItem;
import com.deveagles.be15_deveagles_be.features.items.command.domain.repository.PrimaryItemRepository;
import com.deveagles.be15_deveagles_be.features.items.command.domain.repository.SecondaryItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecondaryItemCommandServiceImplTest {

  private PrimaryItemRepository primaryItemRepository;
  private SecondaryItemRepository secondaryItemRepository;
  private SecondaryItemCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    primaryItemRepository = mock(PrimaryItemRepository.class);
    secondaryItemRepository = mock(SecondaryItemRepository.class);
    service = new SecondaryItemCommandServiceImpl(primaryItemRepository, secondaryItemRepository);
  }

  @Test
  @DisplayName("성공: 유효한 2차 상품 등록 요청 처리")
  void registerSecondaryItem_success() {
    // given
    SecondaryItemRegistRequest request = new SecondaryItemRegistRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("펌");
    request.setSecondaryItemPrice(30000);
    request.setTimeTaken(60);

    PrimaryItem primaryItem =
        PrimaryItem.builder().primaryItemId(1L).category(Category.SERVICE).build();
    when(primaryItemRepository.findById(1L)).thenReturn(Optional.of(primaryItem));

    // when
    service.registerSecondaryItem(request);

    // then
    verify(secondaryItemRepository, times(1)).save(any(SecondaryItem.class));
  }

  @Test
  @DisplayName("실패: 요청 객체가 null인 경우 예외 발생")
  void registerSecondaryItem_nullRequest_throwsException() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.registerSecondaryItem(null));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SECONDARY_ITEM_INPUT);
  }

  @Test
  @DisplayName("실패: 2차 상품명이 비어 있는 경우 예외 발생")
  void registerSecondaryItem_missingName_throwsException() {
    SecondaryItemRegistRequest request = new SecondaryItemRegistRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName(" "); // 공백
    request.setSecondaryItemPrice(20000);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.registerSecondaryItem(request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_NAME_REQUIRED);
  }

  @Test
  @DisplayName("실패: 가격이 null인 경우 예외 발생")
  void registerSecondaryItem_missingPrice_throwsException() {
    SecondaryItemRegistRequest request = new SecondaryItemRegistRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("염색");
    request.setSecondaryItemPrice(null);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.registerSecondaryItem(request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_PRICE_REQUIRED);
  }

  @Test
  @DisplayName("실패: 존재하지 않는 1차 상품 ID인 경우 예외 발생")
  void registerSecondaryItem_primaryItemNotFound_throwsException() {
    SecondaryItemRegistRequest request = new SecondaryItemRegistRequest();
    request.setPrimaryItemId(999L);
    request.setSecondaryItemName("컷");
    request.setSecondaryItemPrice(15000);

    when(primaryItemRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.registerSecondaryItem(request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRIMARY_ITEM_NOT_FOUND);
  }

  @Test
  @DisplayName("실패: 1차 상품이 SERVICE인데 시술 시간이 null인 경우 예외 발생")
  void registerSecondaryItem_missingTimeTakenForService_throwsException() {
    // given
    SecondaryItemRegistRequest request = new SecondaryItemRegistRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("드라이");
    request.setSecondaryItemPrice(10000);
    request.setTimeTaken(null); // 시간 미입력

    PrimaryItem servicePrimaryItem =
        PrimaryItem.builder().primaryItemId(1L).category(Category.SERVICE).build();

    when(primaryItemRepository.findById(1L)).thenReturn(Optional.of(servicePrimaryItem));

    // when
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.registerSecondaryItem(request));

    // then
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_SERVICE_TIME_REQUIRED);
  }

  @Test
  @DisplayName("실패: 2차 상품명이 비어있는 경우")
  void updateSecondaryItem_blankName_throwsException() {
    Long secondaryItemId = 2L;

    SecondaryItemUpdateRequest request = new SecondaryItemUpdateRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName(" ");
    request.setSecondaryItemPrice(30000);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.updateSecondaryItem(secondaryItemId, request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_NAME_REQUIRED);
  }

  @Test
  @DisplayName("실패: 가격이 null인 경우")
  void updateSecondaryItem_missingPrice_throwsException() {
    Long secondaryItemId = 2L;

    SecondaryItemUpdateRequest request = new SecondaryItemUpdateRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("디자인컷");
    request.setSecondaryItemPrice(null);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.updateSecondaryItem(secondaryItemId, request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_PRICE_REQUIRED);
  }

  @Test
  @DisplayName("실패: 존재하지 않는 1차 상품 ID")
  void updateSecondaryItem_primaryItemNotFound_throwsException() {
    Long secondaryItemId = 2L;

    SecondaryItemUpdateRequest request = new SecondaryItemUpdateRequest();
    request.setPrimaryItemId(99L);
    request.setSecondaryItemName("볼륨펌");
    request.setSecondaryItemPrice(30000);

    when(primaryItemRepository.findById(99L)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.updateSecondaryItem(secondaryItemId, request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRIMARY_ITEM_NOT_FOUND);
  }

  @Test
  @DisplayName("실패: 존재하지 않는 2차 상품 ID")
  void updateSecondaryItem_secondaryItemNotFound_throwsException() {
    Long secondaryItemId = 999L;

    SecondaryItemUpdateRequest request = new SecondaryItemUpdateRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("매직펌");
    request.setSecondaryItemPrice(45000);

    PrimaryItem primaryItem =
        PrimaryItem.builder().primaryItemId(1L).category(Category.PRODUCT).build();
    when(primaryItemRepository.findById(1L)).thenReturn(Optional.of(primaryItem));
    when(secondaryItemRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.updateSecondaryItem(secondaryItemId, request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_NOT_FOUND);
  }

  @Test
  @DisplayName("실패: SERVICE인데 시술시간이 null인 경우")
  void updateSecondaryItem_serviceMissingTimeTaken_throwsException() {
    Long secondaryItemId = 2L;

    SecondaryItemUpdateRequest request = new SecondaryItemUpdateRequest();
    request.setPrimaryItemId(1L);
    request.setSecondaryItemName("컷트");
    request.setSecondaryItemPrice(20000);
    request.setTimeTaken(null);

    PrimaryItem primaryItem =
        PrimaryItem.builder().primaryItemId(1L).category(Category.SERVICE).build();
    SecondaryItem existingItem = SecondaryItem.builder().secondaryItemId(secondaryItemId).build();

    when(primaryItemRepository.findById(1L)).thenReturn(Optional.of(primaryItem));
    when(secondaryItemRepository.findById(secondaryItemId)).thenReturn(Optional.of(existingItem));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.updateSecondaryItem(secondaryItemId, request));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SECONDARY_ITEM_SERVICE_TIME_REQUIRED);
  }

  @Test
  @DisplayName("성공: 2차 상품 삭제 수행")
  void deleteSecondaryItem_success() {
    Long id = 1L;
    SecondaryItem item =
        SecondaryItem.builder()
            .secondaryItemId(id)
            .secondaryItemName("디자인컷")
            .primaryItemId(1L) // 1차 상품 ID 연결
            .secondaryItemPrice(30000)
            .build();

    // secondaryItemRepository에서 해당 ID의 아이템을 찾을 수 있도록 설정
    when(secondaryItemRepository.findById(id)).thenReturn(Optional.of(item));

    // 삭제 메서드 호출
    service.deleteSecondaryItem(id);

    // 삭제된 시간이 설정됐는지 확인
    assertNotNull(item.getDeletedAt());

    // secondaryItemRepository에 save 메서드가 호출되었는지 검증
    verify(secondaryItemRepository).save(item);
  }

  @Test
  @DisplayName("실패: 삭제 시 존재하지 않는 ID일 경우 예외 발생")
  void deleteSecondaryItem_notFound_throwsException() {
    Long id = 999L;

    // 존재하지 않는 ID로 찾을 때 Optional.empty()를 반환하도록 설정
    when(secondaryItemRepository.findById(id)).thenReturn(Optional.empty());

    // 예외가 발생하는지 확인
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.deleteSecondaryItem(id));

    // 예외 코드가 맞는지 확인
    assertEquals(ErrorCode.SECONDARY_ITEM_NOT_FOUND, exception.getErrorCode());
  }
}
