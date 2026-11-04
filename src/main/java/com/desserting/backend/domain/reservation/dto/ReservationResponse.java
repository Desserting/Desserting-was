package com.desserting.backend.domain.reservation.dto;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "예약 정보")
public record ReservationResponse(
        @Schema(example = "1") Long reservationId,
        @Schema(example = "3") Long storeId,
        @Schema(example = "카페 올리브") String storeName,
        @Schema(example = "대구 남구 앞산순환로 123") String storeAddress,
        @Schema(example = "2026-11-15") LocalDate reservationDate,
        @Schema(type = "string", example = "15:00") LocalTime reservationTime,
        @Schema(example = "2") Integer guestCount,
        @Schema(example = "PENDING") ReservationStatus status,
        @Schema(example = "예약 대기") String statusDescription,
        @Schema(description = "메뉴 선주문 합산 금액 (4-7 주문 결제 후 반영, 그 전엔 0)", example = "10000") Integer totalAmount,
        @Schema(description = "쿠폰 할인 금액 (4-7 주문 결제 후 반영, 그 전엔 0)", example = "2000") Integer discountAmount,
        @Schema(description = "할인 적용 후 실결제 금액 (4-7 주문 결제 후 반영, 그 전엔 0)", example = "8000") Integer finalAmount,
        @Schema(description = "선택한 쿠폰명", example = "앞산 데이트 2,000원 할인") String couponName,
        String specialRequest,
        LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation r) {
        String couponTitle = r.getCoupon() != null ? r.getCoupon().getCouponName() : null;
        int safeDiscount = r.getDiscountAmount() != null ? r.getDiscountAmount() : 0;
        int safeFinal = r.getFinalAmount() != null ? r.getFinalAmount() : (r.getTotalAmount() - safeDiscount);

        return new ReservationResponse(
                r.getId(),
                r.getStore().getId(),
                r.getStore().getName(),
                r.getStore().getAddress(),
                r.getReservationDate(),
                r.getReservationTime(),
                r.getGuestCount(),
                r.getStatus(),
                r.getStatus().getDescription(),
                r.getTotalAmount(),
                safeDiscount,
                safeFinal,
                couponTitle,
                r.getSpecialRequest(),
                r.getCreatedAt());
    }
}
