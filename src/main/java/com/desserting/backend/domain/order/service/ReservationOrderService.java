package com.desserting.backend.domain.order.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.order.dto.CreateOrderRequest;
import com.desserting.backend.domain.order.dto.OrderItemRequest;
import com.desserting.backend.domain.order.dto.OrderItemResponse;
import com.desserting.backend.domain.order.dto.OrderPreviewRequest;
import com.desserting.backend.domain.order.dto.OrderPreviewResponse;
import com.desserting.backend.domain.order.dto.OrderResponse;
import com.desserting.backend.domain.order.entity.OrderStatus;
import com.desserting.backend.domain.order.entity.PaymentType;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.order.entity.OrderProgressStep;
import com.desserting.backend.domain.order.entity.ReservationOrderItem;
import com.desserting.backend.domain.order.repository.OrderMenuRepository;
import com.desserting.backend.domain.order.repository.OrderReservationRepository;
import com.desserting.backend.domain.order.repository.ReservationOrderRepository;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreCouponRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 4-7. 예약 메뉴 선주문 및 모의 결제 (ORD-01, RSV-04 쿠폰 적용).
 * 흐름: 예약 생성(4-6) → 메뉴 선택 → 주문 확인(미리보기) → 모의 결제(주문 생성) → 점주 준비 상태 변경(4-8).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationOrderService {

    private static final Set<ReservationStatus> ORDERABLE = Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationOrderRepository orderRepository;
    private final OrderReservationRepository reservationRepository;
    private final OrderMenuRepository menuRepository;
    private final StoreCouponRepository couponRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    /** 7-1 주문 확인: 금액만 계산하고 저장하지 않는다 */
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(Long userId, Long reservationId, OrderPreviewRequest request) {
        Reservation reservation = reservationRepository.findByIdWithStore(reservationId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
        validateOrderable(reservation);

        Pricing pricing = price(reservation, request.items(), request.couponId());
        int guests = reservation.getGuestCount() != null ? reservation.getGuestCount() : 1;
        List<Integer> shares = ReservationOrder.splitAmounts(pricing.finalAmount(), guests);
        return new OrderPreviewResponse(
                reservation.getId(),
                pricing.items().stream().map(OrderItemResponse::from).toList(),
                pricing.totalAmount(),
                pricing.coupon() != null ? pricing.coupon().getId() : null,
                pricing.coupon() != null ? pricing.coupon().getCouponName() : null,
                pricing.discountAmount(),
                pricing.finalAmount(),
                reservation.getGuestCount(),
                shares);
    }

    /** 7-2 모의 결제: 결제 승인과 동시에 주문을 만들고 예약 금액을 갱신한다 */
    @Transactional
    public OrderResponse placeOrder(Long userId, Long reservationId, CreateOrderRequest request) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
        validateOrderable(reservation);
        if (orderRepository.existsByReservationId(reservationId)) {
            throw new ExploreException(ExploreErrorCode.ORDER_ALREADY_PAID);
        }

        int splitCount = 1;
        if (request.paymentType() == PaymentType.SPLIT) {
            if (reservation.getGuestCount() == null || reservation.getGuestCount() < 2) {
                throw new ExploreException(ExploreErrorCode.INVALID_SPLIT_PAYMENT);
            }
            splitCount = reservation.getGuestCount();
        }

        Pricing pricing = price(reservation, request.items(), request.couponId());
        ReservationOrder order = ReservationOrder.builder()
                .reservation(reservation)
                .user(reservation.getUser())
                .store(reservation.getStore())
                .coupon(pricing.coupon())
                .couponName(pricing.coupon() != null ? pricing.coupon().getCouponName() : null)
                .totalAmount(pricing.totalAmount())
                .discountAmount(pricing.discountAmount())
                .finalAmount(pricing.finalAmount())
                .paymentType(request.paymentType())
                .splitCount(splitCount)
                .paymentMethod(request.paymentMethod())
                .paymentKey("MOCK-ORD-" + UUID.randomUUID())
                .status(OrderStatus.PAID)
                .paidAt(LocalDateTime.now(clock))
                .build();
        pricing.items().forEach(order::addItem);
        ReservationOrder saved = orderRepository.save(order);

        reservationRepository.applyOrderAmounts(reservation.getId(), pricing.totalAmount(),
                pricing.discountAmount(), pricing.finalAmount(), pricing.coupon());

        log.info("주문·모의 결제 완료: orderId={}, reservationId={}, userId={}, total={}원, discount={}원, final={}원, {}",
                saved.getId(), reservationId, userId, pricing.totalAmount(), pricing.discountAmount(),
                pricing.finalAmount(), request.paymentType());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderOfReservation(Long userId, Long reservationId) {
        return orderRepository.findDetailByReservationId(reservationId)
                .filter(o -> o.getUser().getId().equals(userId))
                .map(OrderResponse::from)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findAllDetailByUserId(userId).stream().map(OrderResponse::from).toList();
    }

    /** 점주(4-8 화면)용: 매장에 들어온 주문 메뉴 내역 */
    @Transactional(readOnly = true)
    public List<OrderResponse> getStoreOrders(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        return orderRepository.findAllDetailByStoreId(storeId).stream().map(OrderResponse::from).toList();
    }

    /**
     * 점주가 주문 준비 단계를 바꾼다 (ORD-03 접수 → 준비 중 → 준비 완료).
     * 준비를 시작하면 예약 상태도 PREPARING 으로 맞춘다. '준비 완료'는 방문 완료(COMPLETED)와 다르므로 예약 상태는 그대로 둔다.
     */
    @Transactional
    public OrderResponse updateProgress(Long storeId, Long orderId, OrderProgressStep step) {
        ReservationOrder order = orderRepository.findById(orderId)
                .filter(o -> o.getStore().getId().equals(storeId))
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ExploreException(ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED, "환불된 주문입니다.");
        }
        Reservation reservation = order.getReservation();
        ReservationStatus rs = reservation.getStatus();
        if (rs == ReservationStatus.CANCELLED || rs == ReservationStatus.COMPLETED) {
            throw new ExploreException(ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED,
                    "취소되었거나 방문 완료된 예약의 주문입니다.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        switch (step) {
            case RECEIVED -> throw new ExploreException(ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED,
                    "주문 접수는 결제 시 자동으로 기록됩니다.");
            case PREPARING -> {
                if (order.getReadyAt() != null) {
                    throw new ExploreException(ExploreErrorCode.ORDER_PROGRESS_NOT_ALLOWED,
                            "이미 준비 완료된 주문입니다.");
                }
                order.markPreparing(now);
            }
            case READY -> order.markReady(now);
        }
        if (rs == ReservationStatus.PENDING || rs == ReservationStatus.CONFIRMED) {
            reservation.updateStatus(ReservationStatus.PREPARING);
        }
        log.info("주문 준비 단계 변경: orderId={}, storeId={}, step={}", orderId, storeId, step);
        return OrderResponse.from(order);
    }

    /** 사용자가 예약을 취소하면 결제 완료 주문을 모의 환불한다 (ReservationService.cancelReservation 에서 호출) */
    @Transactional
    public void refundIfPaid(Long reservationId) {
        orderRepository.findDetailByReservationId(reservationId)
                .filter(o -> o.refund(LocalDateTime.now(clock)))
                .ifPresent(o -> log.info("예약 취소로 주문 모의 환불: orderId={}, reservationId={}, amount={}원",
                        o.getId(), reservationId, o.getFinalAmount()));
    }

    private void validateOrderable(Reservation reservation) {
        if (!ORDERABLE.contains(reservation.getStatus())) {
            throw new ExploreException(ExploreErrorCode.ORDER_NOT_ALLOWED,
                    "예약 대기·확정 상태에서만 주문할 수 있습니다. (현재: " + reservation.getStatus().getDescription() + ")");
        }
    }

    private Pricing price(Reservation reservation, List<OrderItemRequest> itemRequests, Long couponId) {
        Long storeId = reservation.getStore().getId();

        // 같은 메뉴를 여러 줄로 보내면 수량을 합친다 (요청 순서 유지)
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : itemRequests) {
            quantities.merge(item.menuId(), item.quantity(), Integer::sum);
        }

        Map<Long, StoreMenu> menus = menuRepository.findAllByStoreIdAndIdIn(storeId, quantities.keySet()).stream()
                .collect(Collectors.toMap(StoreMenu::getId, Function.identity()));

        List<ReservationOrderItem> items = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            StoreMenu menu = menus.get(e.getKey());
            if (menu == null) {
                throw new ExploreException(ExploreErrorCode.INVALID_ORDER_MENU,
                        "예약한 매장의 메뉴가 아닙니다. (menuId=" + e.getKey() + ")");
            }
            if (Boolean.TRUE.equals(menu.getIsSoldOut())) {
                throw new ExploreException(ExploreErrorCode.MENU_SOLD_OUT, "품절된 메뉴입니다: " + menu.getName());
            }
            ReservationOrderItem item = ReservationOrderItem.of(menu, e.getValue());
            items.add(item);
            total += item.getLineAmount();
        }

        StoreCoupon coupon = resolveCoupon(reservation, couponId);
        int discount = 0;
        if (coupon != null) {
            validateCoupon(coupon, total);
            discount = Math.min(coupon.getDiscountAmount(), total);
        }
        return new Pricing(items, total, coupon, discount, total - discount);
    }

    /** 요청에 쿠폰이 있으면 그 쿠폰, 없으면 예약할 때 고른 쿠폰 */
    private StoreCoupon resolveCoupon(Reservation reservation, Long couponId) {
        Long storeId = reservation.getStore().getId();
        if (couponId != null) {
            return couponRepository.findByIdAndStoreId(couponId, storeId)
                    .orElseThrow(() -> new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "해당 매장의 쿠폰이 아닙니다."));
        }
        return reservation.getCoupon();
    }

    private void validateCoupon(StoreCoupon coupon, int totalAmount) {
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "사용이 중지된 쿠폰입니다.");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now(clock))) {
            throw new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE, "기간이 만료된 쿠폰입니다.");
        }
        int min = coupon.getMinOrderAmount() != null ? coupon.getMinOrderAmount() : 0;
        if (totalAmount < min) {
            throw new ExploreException(ExploreErrorCode.COUPON_NOT_USABLE,
                    String.format("%,d원 이상 주문 시 사용할 수 있는 쿠폰입니다. (현재 %,d원)", min, totalAmount));
        }
    }

    private record Pricing(List<ReservationOrderItem> items, int totalAmount, StoreCoupon coupon,
                           int discountAmount, int finalAmount) {
    }
}
