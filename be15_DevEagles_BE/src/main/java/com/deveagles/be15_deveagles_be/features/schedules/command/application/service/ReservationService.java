package com.deveagles.be15_deveagles_be.features.schedules.command.application.service;

import com.deveagles.be15_deveagles_be.features.customers.command.domain.aggregate.Customer;
import com.deveagles.be15_deveagles_be.features.customers.command.domain.repository.CustomerRepository;
import com.deveagles.be15_deveagles_be.features.schedules.command.application.dto.request.CreateReservationRequest;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.Reservation;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationDetail;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.aggregate.ReservationStatusName;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationDetailRepository;
import com.deveagles.be15_deveagles_be.features.schedules.command.domain.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationDetailRepository reservationDetailRepository;
  private final CustomerRepository customerRepository;

  @Transactional
  public Long createReservation(CreateReservationRequest request) {
    // 1. 고객 조회
    Optional<Customer> optionalCustomer =
        customerRepository.findByPhoneNumberAndShopId(request.customerPhone(), request.shopId());

    Long customerId = null;
    String staffMemo;

    if (optionalCustomer.isPresent()) {
      customerId = optionalCustomer.get().getId();
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

    return reservation.getReservationId();
  }
}
