package com.desserting.backend.domain.order.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.order.dto.OrderProgressStepResponse;
import com.desserting.backend.domain.order.dto.OrderResponse;
import com.desserting.backend.domain.order.entity.OrderProgressStep;
import com.desserting.backend.domain.order.entity.OrderStatus;
import com.desserting.backend.domain.order.entity.PaymentType;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.order.repository.OrderMenuRepository;
import com.desserting.backend.domain.order.repository.OrderReservationRepository;
import com.desserting.backend.domain.order.repository.ReservationOrderRepository;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.repository.StoreCouponRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderProgressServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 16, 18);
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 11, 5, 16, 10);

    @Mock private ReservationOrderRepository orderRepository;
    @Mock private OrderReservationRepository reservationRepository;
    @Mock private OrderMenuRepository menuRepository;
    @Mock private StoreCouponRepository couponRepository;
    @Mock private StoreRepository storeRepository;

    private ReservationOrderService service;
    private Store store;

    @BeforeEach
    void setUp() {
        service = new ReservationOrderService(orderRepository, reservationRepository, menuRepository,
                couponRepository, storeRepository, Clock.fixed(NOW.atZone(KST).toInstant(), KST));
        store = Store.builder().name("아눅 앞산").address("대구 남구")
                .latitude(BigDecimal.ONE).longitude(BigDecimal.ONE).build();
        ReflectionTestUtils.setField(store, "id", 10L);
    }

    @Test
    @DisplayName("결제 직후에는 '주문 접수' 단계이고 접수 시각은 결제 시각이다")
    void initialProgress() {
        OrderResponse res = OrderResponse.from(order(ReservationStatus.PENDING, OrderStatus.PAID));

        assertThat(res.currentStep()).isEqualTo(OrderProgressStep.RECEIVED);
        assertThat(res.progressSteps()).extracting(OrderProgressStepResponse::done).containsExactly(true, false, false);
        assertThat(res.progressSteps().get(0).at()).isEqualTo(PAID_AT);
        assertThat(res.progressSteps()).extracting(OrderProgressStepResponse::label)
                .containsExactly("주문 접수", "준비 중", "준비 완료");
    }

    @Test
    @DisplayName("준비 중으로 바꾸면 시각이 기록되고 예약 상태도 PREPARING 이 된다")
    void markPreparing() {
        ReservationOrder order = order(ReservationStatus.CONFIRMED, OrderStatus.PAID);
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));

        OrderResponse res = service.updateProgress(10L, 5L, OrderProgressStep.PREPARING);

        assertThat(res.currentStep()).isEqualTo(OrderProgressStep.PREPARING);
        assertThat(res.progressSteps().get(1).at()).isEqualTo(NOW);
        assertThat(order.getReservation().getStatus()).isEqualTo(ReservationStatus.PREPARING);
    }

    @Test
    @DisplayName("준비 중을 건너뛰고 준비 완료로 바꾸면 두 단계 모두 같은 시각으로 기록된다")
    void markReadyDirectly() {
        ReservationOrder order = order(ReservationStatus.PENDING, OrderStatus.PAID);
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));

        OrderResponse res = service.updateProgress(10L, 5L, OrderProgressStep.READY);

        assertThat(res.currentStep()).isEqualTo(OrderProgressStep.READY);
        assertThat(order.getPreparingAt()).isEqualTo(NOW);
        assertThat(order.getReadyAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("준비 완료 뒤 준비 중으로 되돌리면 EX407")
    void cannotGoBack() {
        ReservationOrder order = order(ReservationStatus.PREPARING, OrderStatus.PAID);
        order.markReady(PAID_AT);
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateProgress(10L, 5L, OrderProgressStep.PREPARING))
                .isInstanceOf(ExploreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED);
    }

    @Test
    @DisplayName("같은 단계를 다시 보내면 처음 시각을 유지한다")
    void idempotent() {
        ReservationOrder order = order(ReservationStatus.PREPARING, OrderStatus.PAID);
        order.markPreparing(PAID_AT);
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));

        service.updateProgress(10L, 5L, OrderProgressStep.PREPARING);

        assertThat(order.getPreparingAt()).isEqualTo(PAID_AT);
    }

    @Test
    @DisplayName("환불된 주문이나 취소된 예약은 EX407, 다른 매장 주문은 EX405, RECEIVED 지정은 EX407")
    void rejected() {
        given(orderRepository.findById(1L)).willReturn(Optional.of(order(ReservationStatus.PENDING, OrderStatus.REFUNDED)));
        given(orderRepository.findById(2L)).willReturn(Optional.of(order(ReservationStatus.CANCELLED, OrderStatus.PAID)));
        given(orderRepository.findById(3L)).willReturn(Optional.of(order(ReservationStatus.PENDING, OrderStatus.PAID)));

        assertThatThrownBy(() -> service.updateProgress(10L, 1L, OrderProgressStep.READY))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED);
        assertThatThrownBy(() -> service.updateProgress(10L, 2L, OrderProgressStep.READY))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED);
        assertThatThrownBy(() -> service.updateProgress(99L, 3L, OrderProgressStep.READY))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_NOT_FOUND);
        assertThatThrownBy(() -> service.updateProgress(10L, 3L, OrderProgressStep.RECEIVED))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED);
    }

    @Test
    @DisplayName("점주가 예약 상태 API(4-8)로 방문 완료 처리하면 시각 없이 모든 단계가 완료로 보인다")
    void reflectsReservationStatus() {
        OrderResponse res = OrderResponse.from(order(ReservationStatus.COMPLETED, OrderStatus.PAID));

        assertThat(res.currentStep()).isEqualTo(OrderProgressStep.READY);
        assertThat(res.progressSteps()).extracting(OrderProgressStepResponse::done).containsExactly(true, true, true);
        assertThat(res.progressSteps().get(2).at()).isNull();
    }

    @Test
    @DisplayName("환불된 주문은 currentStep 이 null")
    void refundedHasNoCurrentStep() {
        assertThat(OrderResponse.from(order(ReservationStatus.CANCELLED, OrderStatus.REFUNDED)).currentStep()).isNull();
    }

    private ReservationOrder order(ReservationStatus reservationStatus, OrderStatus status) {
        Reservation r = Reservation.builder().store(store)
                .reservationDate(LocalDate.of(2026, 11, 5)).reservationTime(LocalTime.of(17, 0))
                .guestCount(2).status(reservationStatus).build();
        ReflectionTestUtils.setField(r, "id", 100L);
        ReservationOrder o = ReservationOrder.builder().reservation(r).store(store)
                .totalAmount(15000).discountAmount(0).finalAmount(15000)
                .paymentType(PaymentType.COMBINED).splitCount(1).paymentMethod(MockPaymentMethod.CARD)
                .paymentKey("MOCK-ORD-1").status(status).paidAt(PAID_AT).build();
        ReflectionTestUtils.setField(o, "id", 5L);
        return o;
    }
}
