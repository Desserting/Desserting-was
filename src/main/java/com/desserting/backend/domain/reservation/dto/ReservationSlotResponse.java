package com.desserting.backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "예약 시간대 (RSV-02)")
public record ReservationSlotResponse(
        @Schema(description = "시간대 ID (예약 생성 시 사용)", example = "12") Long slotId,
        @Schema(description = "시간", type = "string", example = "15:00") LocalTime time,
        @Schema(description = "정원(인원)", example = "10") int capacity,
        @Schema(description = "남은 인원", example = "6") int remaining,
        @Schema(description = "요청 인원으로 예약 가능 여부 (지난 시간·정원 부족이면 false)", example = "true") boolean available
) {
}
