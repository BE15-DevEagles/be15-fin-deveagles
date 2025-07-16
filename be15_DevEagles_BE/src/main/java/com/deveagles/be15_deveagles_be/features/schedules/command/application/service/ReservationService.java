package com.deveagles.be15_deveagles_be.features.schedules.command.application.service;

import com.deveagles.be15_deveagles_be.common.events.ReservationCreatedEvent;
import com.deveagles.be15_deveagles_be.common.exception.BusinessException;
import com.deveagles.be15_deveagles_be.common.exception.ErrorCode;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerIdResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
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
import java.util.List;
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
            .reservationStatusName(ReservationStatusName.CONFIRMED)
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
            .reservationStatusName(ReservationStatusName.PENDING)
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

    reservation.update(
        request.staffId(),
        ReservationStatusName.valueOf(request.reservationStatusName()),
        request.staffMemo(),
        request.reservationMemo(),
        request.reservationStartAt(),
        request.reservationEndAt());

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

      reservation.changeStatus(request.reservationStatusName());
    }
  }
}
