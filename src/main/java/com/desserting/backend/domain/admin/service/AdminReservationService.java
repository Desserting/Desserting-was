package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.AdminReservationResponse;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationService {

    private final ReservationRepository reservationRepository;

    public List<AdminReservationResponse> getAllReservations() {
        return reservationRepository.findAllWithUserAndStore().stream()
                .map(AdminReservationResponse::from)
                .toList();
    }

    @Transactional
    public AdminReservationResponse updateReservationStatus(Long reservationId, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND, "예약 정보를 찾을 수 없습니다."));

        reservation.updateStatus(status);
        log.info("관리자 예약 상태 변경 완료: reservationId={}, newStatus={}", reservationId, status);
        return AdminReservationResponse.from(reservation);
    }
}
