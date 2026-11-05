package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.AdminReservationResponse;
import com.desserting.backend.domain.order.service.ReservationOrderService;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreReservationSlotRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final ReservationOrderService reservationOrderService;

    @Transactional(readOnly = true)
    public List<AdminReservationResponse> getStoreReservations(Long storeId, ReservationStatus status) {
        if (!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        List<Reservation> reservations;
        if (status != null) {
            reservations = reservationRepository.findAllByStoreIdAndStatus(storeId, status);
        } else {
            reservations = reservationRepository.findAllByStoreIdOrderByReservationDateDesc(storeId);
        }

        return reservations.stream()
                .map(AdminReservationResponse::from)
                .toList();
    }

    @Transactional
    public AdminReservationResponse updateReservationStatus(Long storeId, Long reservationId, ReservationStatus newStatus) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_FOUND, "해당 매장의 예약 내역이 아닙니다.");
        }

        ReservationStatus oldStatus = reservation.getStatus();

        // 1. 만약 취소(CANCELLED)로 변경되는 경우, 해당 슬롯의 정원 반환
        if (newStatus == ReservationStatus.CANCELLED && oldStatus != ReservationStatus.CANCELLED) {
            slotRepository.findByStoreIdAndSlotDateAndSlotTime(
                    storeId, reservation.getReservationDate(), reservation.getReservationTime()
            ).ifPresent(slot -> {
                slot.decreaseReservation();
                log.info("예약 취소로 인한 슬롯 복구: slotId={}, current={}/{}", slot.getId(), slot.getCurrentReservedCount(), slot.getMaxCapacity());
            });
            // 결제된 메뉴 주문(4-7)이 있으면 모의 환불
            reservationOrderService.refundIfPaid(reservationId);
        } else if (oldStatus == ReservationStatus.CANCELLED && newStatus != ReservationStatus.CANCELLED) {
            // 2. 취소된 예약을 다시 유효 상태로 복원할 경우 슬롯 정원 및 과거 일시 검증
            LocalDateTime slotDateTime = LocalDateTime.of(reservation.getReservationDate(), reservation.getReservationTime());
            if (slotDateTime.isBefore(LocalDateTime.now())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 지난 일시의 취소된 예약은 재확정할 수 없습니다.");
            }

            StoreReservationSlot slot = slotRepository.findByStoreIdAndSlotDateAndSlotTime(
                    storeId, reservation.getReservationDate(), reservation.getReservationTime()
            ).orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_SLOT_NOT_FOUND));

            if (slot.getCurrentReservedCount() >= slot.getMaxCapacity() || !Boolean.TRUE.equals(slot.getIsAvailable())) {
                throw new CustomException(ErrorCode.RESERVATION_SLOT_FULL, "해당 시간대 슬롯 정원이 마감되어 예약을 재확정할 수 없습니다.");
            }

            slot.increaseReservation();
            log.info("취소 예약 재확정으로 인한 슬롯 정원 재반영: slotId={}, current={}/{}", slot.getId(), slot.getCurrentReservedCount(), slot.getMaxCapacity());
        }

        reservation.updateStatus(newStatus);
        log.info("예약 상태 변경 완료: reservationId={}, {} -> {}", reservationId, oldStatus, newStatus);
        return AdminReservationResponse.from(reservation);
    }
}
