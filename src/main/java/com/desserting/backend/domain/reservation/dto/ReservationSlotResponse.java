package com.desserting.backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "예약 시간대 (RSV-02)")
public record ReservationSlotResponse(
        @Schema(description = "시간대 ID (예약 생성 시 사용)", example = "12") Long slotId,
        @Schema(description = "시간", type = "string", example = "15:00") LocalTime time,
        @Schema(description = "정원 (예약 가능 팀 수, 예약 1건 = 1팀)", example = "4") int capacity,
        @Schema(description = "남은 팀 수", example = "2") int remaining,
        @Schema(description = "예약 가능 여부 (지난 시간·마감·매장 판매중지면 false)", example = "true") boolean available
) {
}
