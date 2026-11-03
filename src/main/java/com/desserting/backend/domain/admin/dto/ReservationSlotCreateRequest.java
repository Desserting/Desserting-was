package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "예약 슬롯 개설 요청 DTO")
public class ReservationSlotCreateRequest {

    @Schema(description = "예약 일자 (YYYY-MM-DD)", example = "2026-11-10")
    @NotNull(message = "예약 일자는 필수입니다.")
    @FutureOrPresent(message = "예약 일자는 오늘 이후여야 합니다.")
    private LocalDate slotDate;

    @Schema(description = "예약 시간대 (HH:mm:ss)", example = "14:00:00")
    @NotNull(message = "예약 시간은 필수입니다.")
    private LocalTime slotTime;

    @Schema(description = "해당 슬롯 최대 수용 팀/정원 (기본 4)", example = "4")
    @NotNull(message = "수용 인원(팀) 수는 필수입니다.")
    @Min(value = 1, message = "수용 인원은 최소 1팀 이상이어야 합니다.")
    @Builder.Default
    private Integer maxCapacity = 4;
}
