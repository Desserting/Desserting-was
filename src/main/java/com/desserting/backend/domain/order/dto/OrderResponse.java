package com.desserting.backend.domain.order.dto;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.order.entity.OrderStatus;
import com.desserting.backend.domain.order.entity.PaymentType;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "주문·모의 결제 영수증")
public record OrderResponse(
        @Schema(example = "5") Long orderId,
        @Schema(example = "12") Long reservationId,
        @Schema(example = "1") Long storeId,
        @Schema(example = "아눅 앞산 (a.nook Apsan)") String storeName,
        @Schema(example = "2026-11-15") LocalDate reservationDate,
        @Schema(example = "16:00:00") LocalTime reservationTime,
        @Schema(example = "2") Integer guestCount,
        @Schema(description = "매장 준비 상태 (예약 상태를 따름)", example = "PENDING") ReservationStatus reservationStatus,
        @Schema(example = "예약 대기") String reservationStatusDescription,
        List<OrderItemResponse> items,
        @Schema(description = "메뉴 합계", example = "15000") Integer totalAmount,
        @Schema(example = "디저팅 첫 데이트 2,000원 할인") String couponName,
        @Schema(example = "2000") Integer discountAmount,
        @Schema(description = "실결제 금액", example = "13000") Integer finalAmount,
        @Schema(example = "SPLIT") PaymentType paymentType,
        @Schema(description = "1인당 결제 금액 (합산 결제면 1개)", example = "[6500, 6500]") List<Integer> shareAmounts,
        @Schema(example = "CARD") MockPaymentMethod paymentMethod,
        @Schema(description = "모의 결제 승인 번호", example = "MOCK-ORD-6f1c...") String paymentKey,
        @Schema(example = "PAID") OrderStatus orderStatus,
        @Schema(example = "결제 완료(모의)") String orderStatusDescription,
        LocalDateTime paidAt,
        LocalDateTime refundedAt
) {
    public static OrderResponse from(ReservationOrder order) {
        Reservation r = order.getReservation();
        return new OrderResponse(
                order.getId(),
                r.getId(),
                order.getStore().getId(),
                order.getStore().getName(),
                r.getReservationDate(),
                r.getReservationTime(),
                r.getGuestCount(),
                r.getStatus(),
                r.getStatus().getDescription(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount(),
                order.getCouponName(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getPaymentType(),
                order.shareAmounts(),
                order.getPaymentMethod(),
                order.getPaymentKey(),
                order.getStatus(),
                order.getStatus().getDescription(),
                order.getPaidAt(),
                order.getRefundedAt());
    }
}
