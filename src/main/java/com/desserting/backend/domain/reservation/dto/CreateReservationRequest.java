package com.desserting.backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "예약 생성 요청 (RSV-03)")
public record CreateReservationRequest(
        @Schema(description = "예약할 시간대 ID", example = "12")
        @NotNull(message = "시간대(slotId)는 필수입니다.")
        Long slotId,

        @Schema(description = "인원", example = "2")
        @NotNull(message = "인원(guestCount)은 필수입니다.")
        @Min(value = 1, message = "인원은 1명 이상이어야 합니다.")
        @Max(value = 10, message = "인원은 10명 이하여야 합니다.")
        Integer guestCount,

        @Schema(description = "요청 사항", example = "창가 자리 부탁드려요")
        @Size(max = 255, message = "요청 사항은 255자 이하로 입력해주세요.")
        String specialRequest,

        @Schema(description = "사용할 매장 할인쿠폰 ID (선택, 할인은 메뉴 주문 때 적용)", example = "1")
        Long couponId
) {
    public CreateReservationRequest(Long slotId, Integer guestCount, String specialRequest) {
        this(slotId, guestCount, specialRequest, null);
    }
}
