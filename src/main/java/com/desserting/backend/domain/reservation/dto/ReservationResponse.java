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
        @Schema(description = "메뉴 선주문 합산 금액 (4-7 주문에서 갱신)", example = "0") Integer totalAmount,
        String specialRequest,
        LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation r) {
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
                r.getSpecialRequest(),
                r.getCreatedAt());
    }
}
