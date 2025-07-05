package com.deveagles.be15_deveagles_be.features.schedules.command.application.service;

import com.deveagles.be15_deveagles_be.common.events.ReservationCreatedEvent;
import com.deveagles.be15_deveagles_be.features.customers.query.dto.response.CustomerIdResponse;
import com.deveagles.be15_deveagles_be.features.customers.query.service.CustomerQueryService;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.CreateReservationRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.Reservation;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationDetail;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationStatusName;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationDetailRepository;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationRepository;
import jakarta.transaction.Transactional;
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
      customerId = null;
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
}
