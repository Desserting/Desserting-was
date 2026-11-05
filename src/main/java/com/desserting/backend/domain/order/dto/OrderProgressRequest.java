package com.desserting.backend.domain.order.dto;

import com.desserting.backend.domain.order.entity.OrderProgressStep;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 준비 단계 변경 요청 (점주)")
public record OrderProgressRequest(
        @Schema(description = "바꿀 단계 (PREPARING, READY). 주문 접수(RECEIVED)는 결제 때 자동으로 기록됩니다.", example = "PREPARING")
        @NotNull(message = "단계(step)는 필수입니다.")
        OrderProgressStep step
) {
}
