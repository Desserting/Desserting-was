package com.desserting.backend.domain.reservation.service;

import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.order.service.ReservationOrderService;
import com.desserting.backend.domain.reservation.dto.CreateReservationRequest;
import com.desserting.backend.domain.reservation.dto.ReservationDateResponse;
import com.desserting.backend.domain.reservation.dto.ReservationResponse;
import com.desserting.backend.domain.reservation.dto.ReservationSlotResponse;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationSlotBookingRepository;
import com.desserting.backend.domain.reservation.repository.UserReservationRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreCouponRepository;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 4-6. 제휴매장 예약 (RSV-01~04)
 * 슬롯은 매장 관리자(4-8)가 만든 StoreReservationSlot 을 사용하며, 예약 1건이 정원 1팀을 차지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    static final int DEFAULT_DATE_RANGE_DAYS = 14;
    static final int MAX_DATE_RANGE_DAYS = 31;

    private final UserReservationRepository reservationRepository;
    private final ReservationSlotBookingRepository slotRepository;
    private final StoreRepository storeRepository;
    private final StoreCouponRepository storeCouponRepository;
    private final UserRepository userRepository;
    private final ReservationOrderService orderService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final Clock clock;
    private final SimpMessagingTemplate messagingTemplate;

    /** RSV-01: 기간 내 날짜별 예약 가능 여부 (기본 오늘부터 14일) */
    @Transactional(readOnly = true)
    public List<ReservationDateResponse> getAvailableDates(Long storeId, LocalDate from, LocalDate to) {
        validatePartnerStore(storeId);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_DATE_RANGE_DAYS - 1);
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_DATE_RANGE_DAYS) {
            throw new ExploreException(ExploreErrorCode.INVALID_DATE_RANGE,
                    "조회 기간은 시작일 이후이며 최대 " + MAX_DATE_RANGE_DAYS + "일입니다.");
        }
        if (start.isBefore(today)) {
            start = today;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<LocalDate, Integer> remainingByDate = new TreeMap<>();
        for (StoreReservationSlot slot : slotRepository.findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
                storeId, start, end)) {
            int remaining = isBookable(slot, now) ? remaining(slot) : 0;
            remainingByDate.merge(slot.getSlotDate(), remaining, Integer::sum);
        }

        return remainingByDate.entrySet().stream()
                .map(e -> new ReservationDateResponse(e.getKey(), e.getValue() > 0, e.getValue()))
                .toList();
    }

    /** RSV-02: 날짜의 시간대별 잔여 팀 수 */
    @Transactional(readOnly = true)
    public List<ReservationSlotResponse> getSlots(Long storeId, LocalDate date) {
        validatePartnerStore(storeId);
        LocalDateTime now = LocalDateTime.now(clock);

        return slotRepository.findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(storeId, date).stream()
                .map(slot -> new ReservationSlotResponse(
                        slot.getId(),
                        slot.getSlotTime(),
                        slot.getMaxCapacity(),
                        remaining(slot),
                        isBookable(slot, now)))
                .toList();
    }

    /** RSV-03: 예약 생성. 상태는 PENDING(예약 대기)으로 시작하며, 이후 상태 변경은 매장 관리자(4-8)·주문(4-7)이 담당한다. */
    @Transactional
    public ReservationResponse createReservation(Long userId, CreateReservationRequest request) {
        StoreReservationSlot slot = slotRepository.findByIdWithStore(request.slotId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_SLOT_NOT_FOUND));
        Store store = slot.getStore();
        if (!Boolean.TRUE.equals(store.getIsPartner())) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        if (isPast(slot, LocalDateTime.now(clock))) {
            throw new ExploreException(ExploreErrorCode.RESERVATION_SLOT_PASSED);
        }
        if (reservationRepository.existsByUserIdAndStoreIdAndReservationDateAndReservationTimeAndStatusNot(
                userId, store.getId(), slot.getSlotDate(), slot.getSlotTime(), ReservationStatus.CANCELLED)) {
            throw new ExploreException(ExploreErrorCode.DUPLICATE_RESERVATION);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (slotRepository.tryReserve(slot.getId()) == 0) {
            throw new CustomException(ErrorCode.RESERVATION_SLOT_FULL);
        }

        // 쿠폰은 예약에 기억만 해 두고, 최소 주문 금액 확인·할인 계산은 메뉴 주문(4-7) 시점에 한다
        StoreCoupon coupon = request.couponId() != null ? findSelectableCoupon(request.couponId(), store.getId()) : null;

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .user(user)
                .store(store)
                .chatRoomId(request.chatRoomId())
                .reservationDate(slot.getSlotDate())
                .reservationTime(slot.getSlotTime())
                .guestCount(request.guestCount())
                .specialRequest(request.specialRequest())
                .coupon(coupon)
                .build());

        log.info("예약 생성: reservationId={}, userId={}, storeId={}, {} {} ({}명, couponId={}, chatRoomId={})",
                reservation.getId(), userId, store.getId(), slot.getSlotDate(), slot.getSlotTime(),
                request.guestCount(), request.couponId(), request.chatRoomId());

        if (messagingTemplate != null) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "RESERVATION_CREATED");
                payload.put("reservationId", reservation.getId());
                payload.put("storeId", store.getId());
                payload.put("storeName", store.getName());
                payload.put("userNickname", user.getNickname());
                payload.put("reservationDate", slot.getSlotDate().toString());
                payload.put("reservationTime", slot.getSlotTime().toString());
                payload.put("guestCount", request.guestCount());
                payload.put("status", reservation.getStatus().name());
                payload.put("timestamp", LocalDateTime.now().toString());

                messagingTemplate.convertAndSend("/sub/stores/" + store.getId() + "/reservations", payload);
                log.info("신규 예약 매장 채널 실시간 웹소켓 푸시 완료: storeId={}, reservationId={}", store.getId(), reservation.getId());
            } catch (Exception e) {
                log.warn("신규 예약 웹소켓 푸시 실패 (무시됨): {}", e.getMessage());
            }
        }

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
        return ReservationResponse.from(findAccessibleReservation(userId, reservationId));
    }

    /** RSV-04: 예약 취소 및 자리 반환 (예약 대기·확정 상태만) */
    @Transactional
    public ReservationResponse cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = findOwnReservation(userId, reservationId);
        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ExploreException(ExploreErrorCode.RESERVATION_NOT_CANCELLABLE);
        }
        reservation.updateStatus(ReservationStatus.CANCELLED);
        slotRepository.findByStoreIdAndSlotDateAndSlotTime(
                        reservation.getStore().getId(), reservation.getReservationDate(), reservation.getReservationTime())
                .ifPresent(slot -> slotRepository.release(slot.getId()));
        orderService.refundIfPaid(reservationId);

        log.info("예약 취소: reservationId={}, userId={}", reservationId, userId);

        if (messagingTemplate != null) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "RESERVATION_STATUS_CHANGED");
                payload.put("reservationId", reservationId);
                payload.put("storeId", reservation.getStore().getId());
                payload.put("status", ReservationStatus.CANCELLED.name());
                payload.put("timestamp", LocalDateTime.now().toString());

                messagingTemplate.convertAndSend("/sub/reservations/" + reservationId, payload);
                messagingTemplate.convertAndSend("/sub/stores/" + reservation.getStore().getId() + "/reservations", payload);
                log.info("예약 취소 실시간 웹소켓 푸시 완료: reservationId={}", reservationId);
            } catch (Exception e) {
                log.warn("예약 취소 웹소켓 푸시 실패 (무시됨): {}", e.getMessage());
            }
        }

        return ReservationResponse.from(reservation);
    }

    private Reservation findOwnReservation(Long userId, Long reservationId) {
        return reservationRepository.findByIdWithStore(reservationId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /** 예약자 본인 또는 해당 예약이 공유된 채팅방 멤버라면 열람 가능 */
    private Reservation findAccessibleReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithStore(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getUser().getId().equals(userId)) {
            return reservation;
        }

        if (reservation.getChatRoomId() != null
                && chatRoomMemberRepository.findByChatRoomIdAndUserId(reservation.getChatRoomId(), userId).isPresent()) {
            return reservation;
        }

        throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 예약 내역을 조회할 권한이 없습니다.");
    }

    /** 예약 단계에서는 매장·사용중지·만료만 확인 (최소 주문 금액은 주문 시 확인) */
    private StoreCoupon findSelectableCoupon(Long couponId, Long storeId) {
        StoreCoupon coupon = storeCouponRepository.findByIdAndStoreId(couponId, storeId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "해당 매장의 쿠폰이 아닙니다."));
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "사용이 중지된 쿠폰입니다.");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now(clock))) {
            throw new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "기간이 만료된 쿠폰입니다.");
        }
        return coupon;
    }

    private void validatePartnerStore(Long storeId) {
        storeRepository.findById(storeId)
                .filter(s -> Boolean.TRUE.equals(s.getIsPartner()))
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private static int remaining(StoreReservationSlot slot) {
        return Math.max(0, slot.getMaxCapacity() - slot.getCurrentReservedCount());
    }

    private static boolean isBookable(StoreReservationSlot slot, LocalDateTime now) {
        return Boolean.TRUE.equals(slot.getIsAvailable()) && !isPast(slot, now) && remaining(slot) > 0;
    }

    private static boolean isPast(StoreReservationSlot slot, LocalDateTime now) {
        return !LocalDateTime.of(slot.getSlotDate(), slot.getSlotTime()).isAfter(now);
    }
}
