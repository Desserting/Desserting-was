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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final ReservationOrderService reservationOrderService;
    private final SimpMessagingTemplate messagingTemplate;

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

        // 실시간 WebSocket 이벤트 푸시 (유저 앱 및 매장 콘솔 즉시 화면 갱신)
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "RESERVATION_STATUS_CHANGED");
            payload.put("reservationId", reservation.getId());
            payload.put("storeId", storeId);
            payload.put("storeName", reservation.getStore() != null ? reservation.getStore().getName() : "");
            payload.put("status", newStatus.name());
            payload.put("userId", reservation.getUser() != null ? reservation.getUser().getId() : null);
            payload.put("timestamp", LocalDateTime.now().toString());

            // 1. 해당 예약 단건 채널 (유저 내 예약 상세 화면 구독용)
            messagingTemplate.convertAndSend("/sub/reservations/" + reservationId, payload);
            // 2. 유저 개인 채널 (유저 내 예약 목록 화면 구독용)
            if (reservation.getUser() != null) {
                messagingTemplate.convertAndSend("/sub/users/" + reservation.getUser().getId() + "/reservations", payload);
            }
            // 3. 매장 채널 (점주 포털 화면 실시간 동기화용)
            messagingTemplate.convertAndSend("/sub/stores/" + storeId + "/reservations", payload);

            log.info("예약 상태 변경 실시간 웹소켓 이벤트 푸시 성공: reservationId={}, status={}", reservationId, newStatus);
        } catch (Exception e) {
            log.warn("실시간 웹소켓 이벤트 전송 실패 (트랜잭션 미영향): {}", e.getMessage());
        }

        return AdminReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<AdminReservationResponse> getAllReservations(Long storeId, ReservationStatus status) {
        List<Reservation> reservations;
        if (storeId != null && status != null) {
            reservations = reservationRepository.findAllWithUserAndStoreByStoreIdAndStatus(storeId, status);
        } else if (storeId != null) {
            reservations = reservationRepository.findAllWithUserAndStoreByStoreId(storeId);
        } else if (status != null) {
            reservations = reservationRepository.findAllWithUserAndStoreByStatus(status);
        } else {
            reservations = reservationRepository.findAllWithUserAndStore();
        }

        return reservations.stream()
                .map(AdminReservationResponse::from)
                .toList();
    }

    @Transactional
    public AdminReservationResponse updateGlobalReservationStatus(Long reservationId, ReservationStatus newStatus) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
        return updateReservationStatus(reservation.getStore().getId(), reservationId, newStatus);
    }
}
