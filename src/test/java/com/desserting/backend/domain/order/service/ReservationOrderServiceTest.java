package com.desserting.backend.domain.order.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.order.dto.CreateOrderRequest;
import com.desserting.backend.domain.order.dto.OrderItemRequest;
import com.desserting.backend.domain.order.dto.OrderPreviewRequest;
import com.desserting.backend.domain.order.dto.OrderPreviewResponse;
import com.desserting.backend.domain.order.dto.OrderResponse;
import com.desserting.backend.domain.order.entity.OrderStatus;
import com.desserting.backend.domain.order.entity.PaymentType;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.order.repository.OrderMenuRepository;
import com.desserting.backend.domain.order.repository.OrderReservationRepository;
import com.desserting.backend.domain.order.repository.ReservationOrderRepository;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreCouponRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.user.entity.User;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationOrderServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Mock
    private ReservationOrderRepository orderRepository;
    @Mock
    private OrderReservationRepository reservationRepository;
    @Mock
    private OrderMenuRepository menuRepository;
    @Mock
    private StoreCouponRepository couponRepository;
    @Mock
    private StoreRepository storeRepository;

    private ReservationOrderService service;
    private Store store;
    private StoreMenu cake;
    private StoreMenu latte;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(10, 0)).atZone(KST).toInstant(), KST);
        service = new ReservationOrderService(orderRepository, reservationRepository, menuRepository,
                couponRepository, storeRepository, clock);

        store = Store.builder().name("아눅 앞산").address("대구 남구")
                .latitude(BigDecimal.ONE).longitude(BigDecimal.ONE).build();
        ReflectionTestUtils.setField(store, "id", 10L);
        cake = menu(1L, "딸기 케이크", 7500, false);
        latte = menu(2L, "바닐라 라떼", 5500, false);
    }

    @Test
    @DisplayName("미리보기: 같은 메뉴는 수량을 합치고, 최소 주문 금액을 넘으면 쿠폰을 할인한다")
    void preview_mergesItemsAndAppliesCoupon() {
        Reservation reservation = reservation(ReservationStatus.PENDING, 2);
        StoreCoupon coupon = coupon(7L, 2000, 10000, true, null);
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation));
        givenMenus(cake, latte);
        given(couponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon));

        OrderPreviewResponse preview = service.preview(1L, 100L, new OrderPreviewRequest(List.of(
                new OrderItemRequest(1L, 1), new OrderItemRequest(2L, 1), new OrderItemRequest(1L, 1)), 7L));

        assertThat(preview.items()).hasSize(2);
        assertThat(preview.items().get(0).quantity()).isEqualTo(2);
        assertThat(preview.totalAmount()).isEqualTo(20500);   // 7500*2 + 5500
        assertThat(preview.discountAmount()).isEqualTo(2000);
        assertThat(preview.finalAmount()).isEqualTo(18500);
        assertThat(preview.splitShareAmounts()).containsExactly(9250, 9250);
    }

    @Test
    @DisplayName("쿠폰을 따로 안 보내면 예약할 때 고른 쿠폰을 쓴다")
    void preview_usesReservationCoupon() {
        Reservation reservation = reservation(ReservationStatus.PENDING, 2);
        ReflectionTestUtils.setField(reservation, "coupon", coupon(7L, 3000, 0, true, null));
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation));
        givenMenus(latte);

        OrderPreviewResponse preview = service.preview(1L, 100L,
                new OrderPreviewRequest(List.of(new OrderItemRequest(2L, 1)), null));

        assertThat(preview.discountAmount()).isEqualTo(3000);
        assertThat(preview.finalAmount()).isEqualTo(2500);
    }

    @Test
    @DisplayName("최소 주문 금액 미달이면 EX305")
    void preview_couponBelowMinimum() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 2)));
        givenMenus(latte);
        given(couponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon(7L, 2000, 10000, true, null)));

        assertThatThrownBy(() -> service.preview(1L, 100L,
                new OrderPreviewRequest(List.of(new OrderItemRequest(2L, 1)), 7L)))
                .isInstanceOf(ExploreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.COUPON_NOT_USABLE);
    }

    @Test
    @DisplayName("할인액이 메뉴 합계보다 크면 실결제는 0원 (음수 없음)")
    void preview_discountCappedAtTotal() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 2)));
        StoreMenu cookie = menu(3L, "쿠키", 1500, false);
        givenMenus(cookie);
        given(couponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon(7L, 2000, 0, true, null)));

        OrderPreviewResponse preview = service.preview(1L, 100L,
                new OrderPreviewRequest(List.of(new OrderItemRequest(3L, 1)), 7L));

        assertThat(preview.discountAmount()).isEqualTo(1500);
        assertThat(preview.finalAmount()).isZero();
    }

    @Test
    @DisplayName("다른 매장 메뉴는 EX403, 품절 메뉴는 EX404")
    void preview_invalidMenus() {
        given(reservationRepository.findByIdWithStore(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 2)));
        given(menuRepository.findAllByStoreIdAndIdIn(eq(10L), anyCollection())).willReturn(List.of());

        assertThatThrownBy(() -> service.preview(1L, 100L,
                new OrderPreviewRequest(List.of(new OrderItemRequest(99L, 1)), null)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.INVALID_ORDER_MENU);

        givenMenus(menu(4L, "품절 빙수", 9000, true));
        assertThatThrownBy(() -> service.preview(1L, 100L,
                new OrderPreviewRequest(List.of(new OrderItemRequest(4L, 1)), null)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.MENU_SOLD_OUT);
    }

    @Test
    @DisplayName("결제: 주문을 PAID로 저장하고 예약 금액을 갱신한다 (개별 결제는 인원수로 나눔)")
    void placeOrder_success() {
        Reservation reservation = reservation(ReservationStatus.CONFIRMED, 3);
        StoreCoupon coupon = coupon(7L, 2000, 10000, true, null);
        given(reservationRepository.findByIdForUpdate(100L)).willReturn(Optional.of(reservation));
        given(orderRepository.existsByReservationId(100L)).willReturn(false);
        givenMenus(cake, latte);
        given(couponRepository.findByIdAndStoreId(7L, 10L)).willReturn(Optional.of(coupon));
        given(orderRepository.save(any(ReservationOrder.class))).willAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.placeOrder(1L, 100L, new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 1), new OrderItemRequest(2L, 1)), 7L,
                MockPaymentMethod.CARD, PaymentType.SPLIT));

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.totalAmount()).isEqualTo(13000);
        assertThat(response.finalAmount()).isEqualTo(11000);
        assertThat(response.shareAmounts()).containsExactly(3667, 3667, 3666);
        assertThat(response.paymentKey()).startsWith("MOCK-ORD-");
        assertThat(response.items()).extracting("menuName").containsExactly("딸기 케이크", "바닐라 라떼");
        verify(reservationRepository).applyOrderAmounts(100L, 13000, 2000, 11000, coupon);
    }

    @Test
    @DisplayName("이미 결제한 예약은 EX402, 주문은 저장되지 않는다")
    void placeOrder_alreadyPaid() {
        given(reservationRepository.findByIdForUpdate(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 2)));
        given(orderRepository.existsByReservationId(100L)).willReturn(true);

        assertThatThrownBy(() -> service.placeOrder(1L, 100L, combinedRequest()))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_ALREADY_PAID);
        verify(orderRepository, never()).save(any());
        verify(reservationRepository, never()).applyOrderAmounts(anyLong(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("준비 중·완료·취소 예약은 주문할 수 없다 (EX401)")
    void placeOrder_notOrderable() {
        given(reservationRepository.findByIdForUpdate(100L)).willReturn(Optional.of(reservation(ReservationStatus.CANCELLED, 2)));

        assertThatThrownBy(() -> service.placeOrder(1L, 100L, combinedRequest()))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.ORDER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("1인 예약은 개별 결제할 수 없다 (EX406)")
    void placeOrder_splitWithOneGuest() {
        given(reservationRepository.findByIdForUpdate(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 1)));
        given(orderRepository.existsByReservationId(100L)).willReturn(false);

        assertThatThrownBy(() -> service.placeOrder(1L, 100L, new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 1)), null, MockPaymentMethod.CARD, PaymentType.SPLIT)))
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.INVALID_SPLIT_PAYMENT);
    }

    @Test
    @DisplayName("다른 사람 예약에는 주문할 수 없다 (R002)")
    void placeOrder_notOwner() {
        given(reservationRepository.findByIdForUpdate(100L)).willReturn(Optional.of(reservation(ReservationStatus.PENDING, 2)));

        assertThatThrownBy(() -> service.placeOrder(2L, 100L, combinedRequest()))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("예약 취소 시 결제된 주문은 REFUNDED 로 바뀐다")
    void refundIfPaid() {
        ReservationOrder order = ReservationOrder.builder().status(OrderStatus.PAID).finalAmount(10000).splitCount(1).build();
        given(orderRepository.findDetailByReservationId(100L)).willReturn(Optional.of(order));

        service.refundIfPaid(100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getRefundedAt()).isEqualTo(LocalDateTime.of(TODAY, LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("금액 나누기: 나머지는 앞사람부터 1원씩")
    void splitAmounts() {
        assertThat(ReservationOrder.splitAmounts(10001, 2)).containsExactly(5001, 5000);
        assertThat(ReservationOrder.splitAmounts(5000, 1)).containsExactly(5000);
        assertThat(ReservationOrder.splitAmounts(0, 2)).containsExactly(0, 0);
    }

    private CreateOrderRequest combinedRequest() {
        return new CreateOrderRequest(List.of(new OrderItemRequest(1L, 1)), null,
                MockPaymentMethod.EASY_PAY, PaymentType.COMBINED);
    }

    @SuppressWarnings("unchecked")
    private void givenMenus(StoreMenu... menus) {
        given(menuRepository.findAllByStoreIdAndIdIn(eq(10L), any(Collection.class))).willReturn(List.of(menus));
    }

    private Reservation reservation(ReservationStatus status, int guests) {
        User owner = User.builder().build();
        ReflectionTestUtils.setField(owner, "id", 1L);
        Reservation r = Reservation.builder().user(owner).store(store)
                .reservationDate(TODAY.plusDays(1)).reservationTime(LocalTime.of(16, 0))
                .guestCount(guests).status(status).build();
        ReflectionTestUtils.setField(r, "id", 100L);
        return r;
    }

    private StoreMenu menu(Long id, String name, int price, boolean soldOut) {
        StoreMenu m = StoreMenu.builder().store(store).name(name).price(price).isSoldOut(soldOut).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private StoreCoupon coupon(Long id, int discount, int min, boolean active, LocalDate expiry) {
        StoreCoupon c = StoreCoupon.builder().store(store).couponName(discount + "원 할인")
                .discountAmount(discount).minOrderAmount(min).isActive(active).expiryDate(expiry).build();
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
