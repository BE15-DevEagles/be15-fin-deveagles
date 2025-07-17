package com.deveagles.be15_deveagles_be.features.schedules.command.application.service;

import com.deveagles.be15_deveagles_be.common.events.ReservationCreatedEvent;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerDetailResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerIdResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.AutomaticMessageTriggerService;
import com.deveagles.be15_deveagles_be.features.messages.command.application.service.MessageVariableProcessor;
import com.deveagles.be15_deveagles_be.features.messages.command.domain.aggregate.AutomaticEventType;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.CreateReservationFullRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.CreateReservationRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.UpdateReservationRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.UpdateReservationStatusRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.Reservation;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationDetail;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationStatusName;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationDetailRepository;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationDetailRepository reservationDetailRepository;
  private final CustomerQueryService customerQueryService;
  private final ApplicationEventPublisher eventPublisher;
  private final AutomaticMessageTriggerService automaticMessageTriggerService;
  private final MessageVariableProcessor messageVariableProcessor;

  @Transactional
  public Long createReservation(CreateReservationRequest request) {
    // 1. 고객 ID 조회
    Optional<CustomerIdResponse> optionalCustomer =
        customerQueryService.findCustomerIdByPhoneNumber(request.customerPhone(), request.shopId());

    Long customerId = null;
    String staffMemo;

    if (optionalCustomer.isPresent()) {
      customerId = optionalCustomer.get().id();
      staffMemo = null;
    } else {
      staffMemo = "임시 고객: " + request.customerName() + " / " + request.customerPhone();
    }

    // 2. 예약 엔티티 생성
    Reservation reservation =
        Reservation.builder()
            .staffId(request.staffId())
            .shopId(request.shopId())
            .customerId(customerId)
            .reservationStatusName(ReservationStatusName.PENDING)
            .staffMemo(staffMemo)
            .reservationMemo(request.reservationMemo())
            .reservationStartAt(request.reservationStartAt())
            .reservationEndAt(request.reservationEndAt())
            .build();

    reservationRepository.save(reservation);

    // 3. 시술 항목 저장
    for (Long secondaryItemId : request.secondaryItemIds()) {
      ReservationDetail detail =
          ReservationDetail.builder()
              .reservationId(reservation.getReservationId())
              .secondaryItemId(secondaryItemId)
              .build();
      reservationDetailRepository.save(detail);
    }

    ReservationCreatedEvent event =
        new ReservationCreatedEvent(reservation.getShopId(), request.customerName());
    eventPublisher.publishEvent(event);

    return reservation.getReservationId();
  }

  @Transactional
  public Long createFullReservation(Long shopId, CreateReservationFullRequest request) {
    Long customerId = request.customerId(); // null이면 미등록 고객

    Reservation reservation =
        Reservation.builder()
            .staffId(request.staffId())
            .shopId(shopId)
            .customerId(customerId)
            .reservationStatusName(ReservationStatusName.CONFIRMED)
            .staffMemo(request.staffMemo())
            .reservationMemo(request.reservationMemo())
            .reservationStartAt(request.reservationStartAt())
            .reservationEndAt(request.reservationEndAt())
            .build();

    reservationRepository.save(reservation);

    for (Long secondaryItemId : request.secondaryItemIds()) {
      ReservationDetail detail =
          ReservationDetail.builder()
              .reservationId(reservation.getReservationId())
              .secondaryItemId(secondaryItemId)
              .build();
      reservationDetailRepository.save(detail);
    }
    // ✅ 자동발신 처리
    if (customerId != null) {
      Optional<CustomerDetailResponse> optionalCustomer =
          customerQueryService.getCustomerDetail(customerId, shopId);

      if (optionalCustomer.isPresent()) {
        CustomerDetailResponse customerDto = optionalCustomer.get();

        Map<String, String> payload =
            messageVariableProcessor.buildPayload(
                customerDto.getCustomerId(),
                customerDto.getShopId(),
                Map.of(
                    "예약날짜",
                    reservation
                        .getReservationStartAt()
                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));

        automaticMessageTriggerService.triggerAutomaticSend(
            customerDto, AutomaticEventType.RESERVATION_CREATED, payload);
      }
    }

    return reservation.getReservationId();
  }

  @Transactional
  public void updateReservation(Long shopId, Long reservationId, UpdateReservationRequest request) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

    if (!reservation.getShopId().equals(shopId)) {
      throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }
    // 이전 상태/시간 백업
    ReservationStatusName prevStatus = reservation.getReservationStatusName();
    reservation.update(
        request.staffId(),
        ReservationStatusName.valueOf(request.reservationStatusName()),
        request.staffMemo(),
        request.reservationMemo(),
        request.reservationStartAt(),
        request.reservationEndAt());

    ReservationStatusName newStatus = reservation.getReservationStatusName();
    Long customerId = reservation.getCustomerId();
    if (customerId != null) {
      Optional<CustomerDetailResponse> optionalCustomer =
          customerQueryService.getCustomerDetail(customerId, shopId);

      if (optionalCustomer.isPresent()) {
        CustomerDetailResponse customerDto = optionalCustomer.get();

        Map<String, String> payload =
            messageVariableProcessor.buildPayload(
                customerDto.getCustomerId(),
                customerDto.getShopId(),
                Map.of(
                    "예약날짜",
                    reservation
                        .getReservationStartAt()
                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));

        if (prevStatus != ReservationStatusName.CONFIRMED
            && newStatus == ReservationStatusName.CONFIRMED) {
          automaticMessageTriggerService.triggerAutomaticSend(
              customerDto, AutomaticEventType.RESERVATION_CREATED, payload);
        } else if ((prevStatus != ReservationStatusName.CBS
                && prevStatus != ReservationStatusName.CBC)
            && (newStatus == ReservationStatusName.CBS || newStatus == ReservationStatusName.CBC)) {
          automaticMessageTriggerService.triggerAutomaticSend(
              customerDto, AutomaticEventType.RESERVATION_CANCELLED, payload);
        }
      }
    }
    reservationDetailRepository.deleteByReservationId(reservationId);
    List<ReservationDetail> newDetails =
        request.secondaryItemIds().stream()
            .map(
                itemId ->
                    ReservationDetail.builder()
                        .reservationId(reservationId)
                        .secondaryItemId(itemId)
                        .build())
            .toList();

    reservationDetailRepository.saveAll(newDetails);
  }

  @Transactional
  public void deleteReservation(Long shopId, Long reservationId) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

    if (!reservation.getShopId().equals(shopId)) {
      throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    reservation.setDeletedAt(LocalDateTime.now());
  }

  @Transactional
  public void changeReservationStatuses(
      Long shopId, List<UpdateReservationStatusRequest> requestList) {
    for (UpdateReservationStatusRequest request : requestList) {
      Reservation reservation =
          reservationRepository
              .findById(request.reservationId())
              .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

      if (!reservation.getShopId().equals(shopId)) {
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
      }

      if (reservation.getReservationStatusName() == ReservationStatusName.PAID) {
        throw new BusinessException(ErrorCode.MODIFY_NOT_ALLOWED_FOR_PAID_RESERVATION);
      }
      ReservationStatusName prevStatus = reservation.getReservationStatusName();
      reservation.changeStatus(request.reservationStatusName());
      ReservationStatusName newStatus = reservation.getReservationStatusName();

      // 고객 ID가 없는 경우 자동발신 스킵
      Long customerId = reservation.getCustomerId();
      if (customerId == null) {
        continue;
      }

      // 상태 변경이 확정 or 취소일 경우 자동발신 처리
      boolean isConfirmedTransition =
          prevStatus != ReservationStatusName.CONFIRMED
              && newStatus == ReservationStatusName.CONFIRMED;

      boolean isCancelledTransition =
          (prevStatus != ReservationStatusName.CBS && prevStatus != ReservationStatusName.CBC)
              && (newStatus == ReservationStatusName.CBS || newStatus == ReservationStatusName.CBC);

      if (isConfirmedTransition || isCancelledTransition) {
        Optional<CustomerDetailResponse> optionalCustomer =
            customerQueryService.getCustomerDetail(customerId, shopId);

        if (optionalCustomer.isPresent()) {
          CustomerDetailResponse customerDto = optionalCustomer.get();

          Map<String, String> payload =
              messageVariableProcessor.buildPayload(
                  customerDto.getCustomerId(),
                  customerDto.getShopId(),
                  Map.of(
                      "예약날짜",
                      reservation
                          .getReservationStartAt()
                          .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));

          AutomaticEventType eventType =
              isConfirmedTransition
                  ? AutomaticEventType.RESERVATION_CREATED
                  : AutomaticEventType.RESERVATION_CANCELLED;

          automaticMessageTriggerService.triggerAutomaticSend(customerDto, eventType, payload);
        }
      }
    }
  }
}
