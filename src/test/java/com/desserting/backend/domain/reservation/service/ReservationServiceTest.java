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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationSlotRepository slotRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;

    private ReservationService reservationService;
    private Store store;

    @BeforeEach
    void setUp() {
        // 현재 시각: 2026-11-03 14:30 KST
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(14, 30)).atZone(KST).toInstant(), KST);
        reservationService = new ReservationService(reservationRepository, slotRepository, storeRepository, userRepository, clock);

        store = Store.builder()
                .name("카페 올리브")
                .address("대구 남구 앞산순환로 451")
                .latitude(BigDecimal.valueOf(35.8297))
                .longitude(BigDecimal.valueOf(128.5871))
                .build();
        ReflectionTestUtils.setField(store, "id", 10L);
    }

    @Test
    @DisplayName("시간대 조회 시 지난 시간과 인원 부족 시간은 available=false")
    void getSlots_marksPastAndFull() {
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(slotRepository.findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(10L, TODAY)).willReturn(List.of(
                slot(1L, TODAY, 14, 10, 0),   // 지남
                slot(2L, TODAY, 15, 10, 9),   // 1명 남음
                slot(3L, TODAY, 16, 10, 2)));

        List<ReservationSlotResponse> slots = reservationService.getSlots(10L, TODAY, 2);

        assertThat(slots).extracting(ReservationSlotResponse::available).containsExactly(false, false, true);
        assertThat(slots.get(1).remaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("날짜 조회는 지난 시간대를 빼고 남은 좌석을 합산한다")
    void getAvailableDates_sumsRemaining() {
        LocalDate tomorrow = TODAY.plusDays(1);
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(slotRepository.findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(10L, TODAY, TODAY.plusDays(13)))
                .willReturn(List.of(
                        slot(1L, TODAY, 11, 10, 0),      // 지남 → 0
                        slot(2L, TODAY, 20, 10, 10),     // 마감 → 0
                        slot(3L, tomorrow, 12, 10, 3),
                        slot(4L, tomorrow, 13, 10, 0)));

        List<ReservationDateResponse> dates = reservationService.getAvailableDates(10L, null, null);

        assertThat(dates).hasSize(2);
        assertThat(dates.get(0).available()).isFalse();
        assertThat(dates.get(1).remainingSeats()).isEqualTo(17);
    }

    @Test
    @DisplayName("조회 기간이 31일을 넘으면 INVALID_DATE_RANGE")
    void getAvailableDates_rangeTooLong() {
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));

        assertThatThrownBy(() -> reservationService.getAvailableDates(10L, TODAY, TODAY.plusDays(40)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("예약 생성 시 정원을 차감하고 PENDING 예약을 저장한다")
    void createReservation_success() {
        ReservationSlot slot = slot(5L, TODAY.plusDays(1), 15, 10, 0);
        User user = User.builder().email("me@desserting.com").nickname("me").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot));
        given(reservationRepository.existsByUserIdAndSlotIdAndStatusNot(1L, 5L, ReservationStatus.CANCELLED)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(slotRepository.tryReserve(5L, 2)).willReturn(1);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, "창가 자리"));

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.reservationTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(response.guestCount()).isEqualTo(2);
        assertThat(response.storeName()).isEqualTo("카페 올리브");
    }

    @Test
    @DisplayName("정원이 부족하면 RESERVATION_SLOT_FULL, 예약은 저장되지 않는다")
    void createReservation_full() {
        ReservationSlot slot = slot(5L, TODAY.plusDays(1), 15, 10, 9);
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot));
        given(reservationRepository.existsByUserIdAndSlotIdAndStatusNot(1L, 5L, ReservationStatus.CANCELLED)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().build()));
        given(slotRepository.tryReserve(5L, 2)).willReturn(0);

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_SLOT_FULL);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("지난 시간대는 예약할 수 없다")
    void createReservation_pastSlot() {
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TODAY, 13, 10, 0)));

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_SLOT_PASSED);
        verify(slotRepository, never()).tryReserve(anyLong(), anyInt());
    }

    @Test
    @DisplayName("같은 시간대 중복 예약은 DUPLICATE_RESERVATION")
    void createReservation_duplicate() {
        given(slotRepository.findByIdWithStore(5L)).willReturn(Optional.of(slot(5L, TODAY.plusDays(1), 15, 10, 2)));
        given(reservationRepository.existsByUserIdAndSlotIdAndStatusNot(1L, 5L, ReservationStatus.CANCELLED)).willReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(1L, new CreateReservationRequest(5L, 2, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESERVATION);
    }

    @Test
    @DisplayName("예약 취소 시 상태를 CANCELLED로 바꾸고 좌석을 반환한다")
    void cancelReservation_releasesSeats() {
        Reservation reservation = reservation(100L, 1L, ReservationStatus.PENDING);
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.cancelReservation(1L, 100L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(slotRepository).release(5L, 2);
    }

    @Test
    @DisplayName("다른 사람의 예약은 조회·취소할 수 없다")
    void cancelReservation_notOwner() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(100L, 2L, ReservationStatus.PENDING)));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 100L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void cancelReservation_alreadyCancelled() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(100L, 1L, ReservationStatus.CANCELLED)));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 100L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_CANCELLABLE);
        verify(slotRepository, never()).release(anyLong(), anyInt());
    }

    private ReservationSlot slot(Long id, LocalDate date, int hour, int capacity, int reserved) {
        ReservationSlot slot = ReservationSlot.builder()
                .store(store).slotDate(date).slotTime(LocalTime.of(hour, 0))
                .capacity(capacity).reservedCount(reserved).build();
        ReflectionTestUtils.setField(slot, "id", id);
        return slot;
    }

    private Reservation reservation(Long id, Long ownerId, ReservationStatus status) {
        User owner = User.builder().build();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        Reservation r = Reservation.builder()
                .user(owner).store(store).slot(slot(5L, TODAY.plusDays(1), 15, 10, 2))
                .reservationDate(TODAY.plusDays(1)).reservationTime(LocalTime.of(15, 0))
                .guestCount(2).status(status).build();
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }
}
