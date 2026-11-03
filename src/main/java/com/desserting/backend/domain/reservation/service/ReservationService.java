package com.desserting.backend.domain.reservation.service;

import com.desserting.backend.domain.reservation.dto.CreateReservationRequest;
import com.desserting.backend.domain.reservation.dto.ReservationDateResponse;
import com.desserting.backend.domain.reservation.dto.ReservationResponse;
import com.desserting.backend.domain.reservation.dto.ReservationSlotResponse;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationSlot;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.domain.reservation.repository.ReservationSlotRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 4-6. 제휴매장 예약 (RSV-01~04)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    static final int DEFAULT_DATE_RANGE_DAYS = 14;
    static final int MAX_DATE_RANGE_DAYS = 31;

    private final ReservationRepository reservationRepository;
    private final ReservationSlotRepository slotRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** RSV-01: 기간 내 날짜별 예약 가능 여부 (기본 오늘부터 14일) */
    @Transactional(readOnly = true)
    public List<ReservationDateResponse> getAvailableDates(Long storeId, LocalDate from, LocalDate to) {
        validatePartnerStore(storeId);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_DATE_RANGE_DAYS - 1);
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_DATE_RANGE_DAYS) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE,
                    "조회 기간은 시작일 이후이며 최대 " + MAX_DATE_RANGE_DAYS + "일입니다.");
        }
        if (start.isBefore(today)) {
            start = today;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<LocalDate, Integer> remainingByDate = new TreeMap<>();
        for (ReservationSlot slot : slotRepository.findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
                storeId, start, end)) {
            int remaining = isPast(slot, now) ? 0 : slot.remaining();
            remainingByDate.merge(slot.getSlotDate(), remaining, Integer::sum);
        }

        return remainingByDate.entrySet().stream()
                .map(e -> new ReservationDateResponse(e.getKey(), e.getValue() > 0, e.getValue()))
                .toList();
    }

    /** RSV-02: 날짜의 시간대별 잔여 인원 */
    @Transactional(readOnly = true)
    public List<ReservationSlotResponse> getSlots(Long storeId, LocalDate date, Integer guestCount) {
        validatePartnerStore(storeId);
        int guests = guestCount == null || guestCount < 1 ? 1 : guestCount;
        LocalDateTime now = LocalDateTime.now(clock);

        return slotRepository.findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(storeId, date).stream()
                .map(slot -> new ReservationSlotResponse(
                        slot.getId(),
                        slot.getSlotTime(),
                        slot.getCapacity(),
                        slot.remaining(),
                        !isPast(slot, now) && slot.remaining() >= guests))
                .toList();
    }

    /** RSV-03: 예약 생성. 상태는 PENDING(결제 대기)으로 시작한다. */
    @Transactional
    public ReservationResponse createReservation(Long userId, CreateReservationRequest request) {
        ReservationSlot slot = slotRepository.findByIdWithStore(request.slotId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_SLOT_NOT_FOUND));
        Store store = slot.getStore();
        if (!Boolean.TRUE.equals(store.getIsPartner())) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        if (isPast(slot, LocalDateTime.now(clock))) {
            throw new CustomException(ErrorCode.RESERVATION_SLOT_PASSED);
        }
        if (reservationRepository.existsByUserIdAndSlotIdAndStatusNot(userId, slot.getId(), ReservationStatus.CANCELLED)) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (slotRepository.tryReserve(slot.getId(), request.guestCount()) == 0) {
            throw new CustomException(ErrorCode.RESERVATION_SLOT_FULL);
        }

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .user(user)
                .store(store)
                .slot(slot)
                .reservationDate(slot.getSlotDate())
                .reservationTime(slot.getSlotTime())
                .guestCount(request.guestCount())
                .specialRequest(request.specialRequest())
                .build());

        log.info("예약 생성: reservationId={}, userId={}, storeId={}, {} {} ({}명)", reservation.getId(), userId,
                store.getId(), slot.getSlotDate(), slot.getSlotTime(), request.guestCount());
        return ReservationResponse.from(reservation);
    }

    /** RSV-03: 내 예약 내역 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long userId) {
        return reservationRepository.findMyReservations(userId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getMyReservation(Long userId, Long reservationId) {
        return ReservationResponse.from(findOwnReservation(userId, reservationId));
    }

    /** RSV-04: 예약 취소 및 좌석 반환 */
    @Transactional
    public ReservationResponse cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = findOwnReservation(userId, reservationId);
        if (!reservation.isCancellable()) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_CANCELLABLE);
        }
        reservation.cancel();
        if (reservation.getSlot() != null) {
            slotRepository.release(reservation.getSlot().getId(), reservation.getGuestCount());
        }
        log.info("예약 취소: reservationId={}, userId={}", reservationId, userId);
        return ReservationResponse.from(reservation);
    }

    private Reservation findOwnReservation(Long userId, Long reservationId) {
        return reservationRepository.findByIdWithStore(reservationId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validatePartnerStore(Long storeId) {
        storeRepository.findById(storeId)
                .filter(s -> Boolean.TRUE.equals(s.getIsPartner()))
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private static boolean isPast(ReservationSlot slot, LocalDateTime now) {
        return !LocalDateTime.of(slot.getSlotDate(), slot.getSlotTime()).isAfter(now);
    }
}
