package com.desserting.backend.domain.reservation.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Mock
    private UserReservationRepository reservationRepository;
    @Mock
    private ReservationSlotBookingRepository slotRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreCouponRepository storeCouponRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationOrderService orderService;

    private ReservationService reservationService;
    private Store store;

    @BeforeEach
    void setUp() {
        // 현재 시각: 2026-11-03 14:30 KST
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(14, 30)).atZone(KST).toInstant(), KST);
        reservationService = new ReservationService(reservationRepository, slotRepository, storeRepository, storeCouponRepository, userRepository, orderService, clock);

        store = Store.builder()
                .name("아눅 앞산")
                .address("대구 남구 앞산순환로 459")
                .latitude(BigDecimal.valueOf(35.8324))
                .longitude(BigDecimal.valueOf(128.5831))
                .build();
        ReflectionTestUtils.setField(store, "id", 10L);
    }

    @Test
    @DisplayName("시간대 조회 시 지난 시간·마감·판매중지 시간대는 available=false")
    void getSlots_marksUnavailable() {
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(slotRepository.findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(10L, TODAY)).willReturn(List.of(
                slot(1L, TODAY, 14, 4, 0, true),    // 지남
                slot(2L, TODAY, 16, 4, 4, false),   // 마감
                slot(3L, TODAY, 18, 4, 1, false),   // 점주가 판매중지
                slot(4L, TODAY, 20, 4, 1, true)));

        List<ReservationSlotResponse> slots = reservationService.getSlots(10L, TODAY);

        assertThat(slots).extracting(ReservationSlotResponse::available).containsExactly(false, false, false, true);
        assertThat(slots.get(3).remaining()).isEqualTo(3);
        assertThat(slots.get(3).capacity()).isEqualTo(4);
    }

    @Test
    @DisplayName("날짜 조회는 예약 불가 시간대를 빼고 남은 팀 수를 합산한다")
    void getAvailableDates_sumsRemaining() {
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(slotRepository.findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(10L, TODAY, TODAY.plusDays(13)))
                .willReturn(List.of(
                        slot(1L, TODAY, 14, 4, 0, true),      // 지남 → 0
                        slot(2L, TODAY, 20, 4, 4, false),     // 마감 → 0
                        slot(3L, TOMORROW, 14, 4, 1, true),
                        slot(4L, TOMORROW, 16, 4, 0, true)));

        List<ReservationDateResponse> dates = reservationService.getAvailableDates(10L, null, null);

        assertThat(dates).hasSize(2);
        assertThat(dates.get(0).available()).isFalse();
        assertThat(dates.get(1).remainingCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("조회 기간이 31일을 넘으면 INVALID_DATE_RANGE")
    void getAvailableDates_rangeTooLong() {
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));

        assertThatThrownBy(() -> reservationService.getAvailableDates(10L, TODAY, TODAY.plusDays(40)))
                .isInstanceOf(ExploreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("예약 생성 시 슬롯 1팀을 차감하고 PENDING 예약을 저장한다")
    void createReservation_success() {
        StoreReservationSlot slot = slot(5L, TOMORROW, 16, 4, 0, true);
        User user = User.builder().email("me@desserting.com").nickname("me").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot));
        given(reservationRepository.existsByUserIdAndStoreIdAndReservationDateAndReservationTimeAndStatusNot(
                1L, 10L, TOMORROW, LocalTime.of(16, 0), ReservationStatus.CANCELLED)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(slotRepository.tryReserve(5L)).willReturn(1);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, "창가 자리"));

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.reservationTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(response.guestCount()).isEqualTo(2);
        assertThat(response.storeName()).isEqualTo("아눅 앞산");
    }

    @Test
    @DisplayName("마감된 시간대면 RESERVATION_SLOT_FULL, 예약은 저장되지 않는다")
    void createReservation_full() {
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TOMORROW, 16, 4, 4, false)));
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().build()));
        given(slotRepository.tryReserve(5L)).willReturn(0);

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_SLOT_FULL);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("지난 시간대는 예약할 수 없다")
    void createReservation_pastSlot() {
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TODAY, 14, 4, 0, true)));

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.RESERVATION_SLOT_PASSED);
        verify(slotRepository, never()).tryReserve(anyLong());
    }

    @Test
    @DisplayName("같은 매장·시간대 중복 예약은 DUPLICATE_RESERVATION")
    void createReservation_duplicate() {
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TOMORROW, 16, 4, 1, true)));
        given(reservationRepository.existsByUserIdAndStoreIdAndReservationDateAndReservationTimeAndStatusNot(
                1L, 10L, TOMORROW, LocalTime.of(16, 0), ReservationStatus.CANCELLED)).willReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.DUPLICATE_RESERVATION);
    }

    @Test
    @DisplayName("예약 취소 시 CANCELLED로 바꾸고 해당 시간대 자리를 반환한다")
    void cancelReservation_releasesSlot() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(100L, 1L, ReservationStatus.PENDING)));
        given(slotRepository.findByStoreIdAndSlotDateAndSlotTime(10L, TOMORROW, LocalTime.of(16, 0)))
                .willReturn(Optional.of(slot(5L, TOMORROW, 16, 4, 1, true)));

        ReservationResponse response = reservationService.cancelReservation(1L, 100L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(slotRepository).release(5L);
        verify(orderService).refundIfPaid(100L);
    }

    @Test
    @DisplayName("최소 주문 금액이 있는 쿠폰도 예약 때는 고를 수 있고, 할인은 주문 전까지 0원")
    void createReservation_couponWithMinOrderAmount() {
        StoreReservationSlot slot = slot(5L, TOMORROW, 16, 4, 0, true);
        StoreCoupon coupon = StoreCoupon.builder().store(store).couponName("2,000원 할인")
                .discountAmount(2000).minOrderAmount(10000).build();
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot));
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().build()));
        given(slotRepository.tryReserve(5L)).willReturn(1);
        given(storeCouponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null, 7L));

        assertThat(response.couponName()).isEqualTo("2,000원 할인");
        assertThat(response.discountAmount()).isZero();
        assertThat(response.finalAmount()).isZero();
    }

    @Test
    @DisplayName("만료된 쿠폰으로는 예약할 수 없다 (EX305)")
    void createReservation_expiredCoupon() {
        StoreCoupon coupon = StoreCoupon.builder().store(store).couponName("지난 쿠폰")
                .discountAmount(2000).expiryDate(TODAY.minusDays(1)).build();
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TOMORROW, 16, 4, 0, true)));
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().build()));
        given(slotRepository.tryReserve(5L)).willReturn(1);
        given(storeCouponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null, 7L)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.COUPON_NOT_USABLE);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 사람의 예약은 조회·취소할 수 없다")
    void cancelReservation_notOwner() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(100L, 2L, ReservationStatus.PENDING)));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 100L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("준비 중·완료·취소된 예약은 사용자가 취소할 수 없다")
    void cancelReservation_notCancellable() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(100L, 1L, ReservationStatus.PREPARING)));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 100L))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.RESERVATION_NOT_CANCELLABLE);
        verify(slotRepository, never()).release(anyLong());
    }

    private StoreReservationSlot slot(Long id, LocalDate date, int hour, int max, int reserved, boolean available) {
        StoreReservationSlot slot = StoreReservationSlot.builder()
                .store(store).slotDate(date).slotTime(LocalTime.of(hour, 0))
                .maxCapacity(max).currentReservedCount(reserved).isAvailable(available).build();
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    private Reservation reservation(Long id, Long ownerId, ReservationStatus status) {
        User owner = User.builder().build();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        Reservation r = Reservation.builder()
                .user(owner).store(store)
                .reservationDate(TOMORROW).reservationTime(LocalTime.of(16, 0))
                .guestCount(2).status(status).build();
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }
}
