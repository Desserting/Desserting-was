package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.AdminReservationResponse;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreReservationSlotRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.order.service.ReservationOrderService;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreReservationSlotRepository slotRepository;

    @Mock
    private ReservationOrderService reservationOrderService;

    @InjectMocks
    private AdminOrderService adminOrderService;

    @Test
    @DisplayName("예약 상태 변경(PREPARING 메뉴 준비 중) 성공")
    void updateReservationStatus_ToPreparing() {
        // given
        Long storeId = 1L;
        Long reservationId = 100L;

        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", storeId);

        User user = User.builder().nickname("딸기케이크").build();
        ReflectionTestUtils.setField(user, "id", 10L);

        Reservation reservation = Reservation.builder()
                .store(store)
                .user(user)
                .reservationDate(LocalDate.now().plusDays(1))
                .reservationTime(LocalTime.of(15, 0))
                .status(ReservationStatus.CONFIRMED)
                .totalAmount(25000)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when
        AdminReservationResponse response = adminOrderService.updateReservationStatus(storeId, reservationId, ReservationStatus.PREPARING);

        // then
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PREPARING);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PREPARING);
    }

    @Test
    @DisplayName("예약 취소(CANCELLED) 시 해당 시간대 슬롯 정원이 복구된다")
    void updateReservationStatus_ToCancelled_RestoresSlot() {
        // given
        Long storeId = 1L;
        Long reservationId = 100L;
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(15, 0);

        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", storeId);

        User user = User.builder().nickname("딸기케이크").build();
        ReflectionTestUtils.setField(user, "id", 10L);

        Reservation reservation = Reservation.builder()
                .store(store)
                .user(user)
                .reservationDate(date)
                .reservationTime(time)
                .status(ReservationStatus.CONFIRMED)
                .totalAmount(25000)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        StoreReservationSlot slot = StoreReservationSlot.builder()
                .store(store)
                .slotDate(date)
                .slotTime(time)
                .maxCapacity(4)
                .currentReservedCount(4)
                .isAvailable(false)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(slotRepository.findByStoreIdAndSlotDateAndSlotTime(storeId, date, time)).willReturn(Optional.of(slot));

        // when
        AdminReservationResponse response = adminOrderService.updateReservationStatus(storeId, reservationId, ReservationStatus.CANCELLED);

        // then
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(slot.getCurrentReservedCount()).isEqualTo(3);
        assertThat(slot.getIsAvailable()).isTrue();
        verify(reservationOrderService).refundIfPaid(reservationId);
    }

    @Test
    @DisplayName("취소되었던 예약을 다시 확정(CONFIRMED)으로 복구할 때 슬롯의 정원이 다시 점유된다")
    void updateReservationStatus_RestoreCancelled_IncreasesSlotReservation() {
        // given
        Long storeId = 1L;
        Long reservationId = 100L;
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(14, 0);

        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", storeId);

        User user = User.builder().nickname("딸기케이크").build();
        ReflectionTestUtils.setField(user, "id", 10L);

        Reservation reservation = Reservation.builder()
                .store(store)
                .user(user)
                .reservationDate(date)
                .reservationTime(time)
                .status(ReservationStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        StoreReservationSlot slot = StoreReservationSlot.builder()
                .maxCapacity(4)
                .currentReservedCount(2)
                .isAvailable(true)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(slotRepository.findByStoreIdAndSlotDateAndSlotTime(storeId, date, time)).willReturn(Optional.of(slot));

        // when
        AdminReservationResponse response = adminOrderService.updateReservationStatus(storeId, reservationId, ReservationStatus.CONFIRMED);

        // then
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(slot.getCurrentReservedCount()).isEqualTo(3);
        verify(reservationOrderService, never()).refundIfPaid(anyLong());
    }

    @Test
    @DisplayName("취소된 예약 재확정 시 슬롯이 이미 만석이면 RESERVATION_SLOT_FULL 예외 발생")
    void updateReservationStatus_RestoreCancelled_ThrowsWhenSlotFull() {
        // given
        Long storeId = 1L;
        Long reservationId = 100L;
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(14, 0);

        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", storeId);

        Reservation reservation = Reservation.builder()
                .store(store)
                .reservationDate(date)
                .reservationTime(time)
                .status(ReservationStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        StoreReservationSlot slot = StoreReservationSlot.builder()
                .maxCapacity(4)
                .currentReservedCount(4)
                .isAvailable(false)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(slotRepository.findByStoreIdAndSlotDateAndSlotTime(storeId, date, time)).willReturn(Optional.of(slot));

        // when & then
        assertThatThrownBy(() -> adminOrderService.updateReservationStatus(storeId, reservationId, ReservationStatus.CONFIRMED))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_SLOT_FULL);
    }

    @Test
    @DisplayName("과거 일시의 취소된 예약은 재확정할 수 없다")
    void updateReservationStatus_RestoreCancelled_ThrowsWhenPastDate() {
        // given
        Long storeId = 1L;
        Long reservationId = 100L;
        LocalDate date = LocalDate.now().minusDays(1);
        LocalTime time = LocalTime.of(14, 0);

        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", storeId);

        Reservation reservation = Reservation.builder()
                .store(store)
                .reservationDate(date)
                .reservationTime(time)
                .status(ReservationStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> adminOrderService.updateReservationStatus(storeId, reservationId, ReservationStatus.CONFIRMED))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }
}
