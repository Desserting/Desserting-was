package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "예약/주문 상태 변경 요청 DTO")
public class ReservationStatusUpdateRequest {

    @Schema(description = "변경할 상태 (CONFIRMED: 예약확정, PREPARING: 메뉴준비중, COMPLETED: 방문완료, CANCELLED: 예약취소)", example = "PREPARING")
    @NotNull(message = "변경 상태값은 필수입니다.")
    private ReservationStatus status;
}
