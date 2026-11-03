package com.desserting.backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "예약 가능 날짜 (RSV-01, 달력 표시용)")
public record ReservationDateResponse(
        @Schema(description = "날짜", example = "2026-11-15") LocalDate date,
        @Schema(description = "예약 가능한 시간대가 하나라도 있는지", example = "true") boolean available,
        @Schema(description = "해당 날짜 남은 예약 가능 팀 수 합계 (지난·마감 시간대 제외)", example = "12") int remainingCount
) {
}
