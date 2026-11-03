package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "예약 슬롯 정보 응답 DTO")
public class ReservationSlotResponse {

    @Schema(description = "슬롯 ID", example = "1")
    private Long slotId;

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "예약 일자", example = "2026-11-10")
    private LocalDate slotDate;

    @Schema(description = "예약 시간", example = "14:00:00")
    private LocalTime slotTime;

    @Schema(description = "총 정원 (수용 가능 팀 수)", example = "4")
    private Integer maxCapacity;

    @Schema(description = "현재 예약 확정 건수", example = "1")
    private Integer currentReservedCount;

    @Schema(description = "잔여 예약 가능 여부", example = "true")
    private Boolean isAvailable;

    public static ReservationSlotResponse from(StoreReservationSlot slot) {
        return ReservationSlotResponse.builder()
                .slotId(slot.getId())
                .storeId(slot.getStore().getId())
                .slotDate(slot.getSlotDate())
                .slotTime(slot.getSlotTime())
                .maxCapacity(slot.getMaxCapacity())
                .currentReservedCount(slot.getCurrentReservedCount())
                .isAvailable(slot.getIsAvailable())
                .build();
    }
}
