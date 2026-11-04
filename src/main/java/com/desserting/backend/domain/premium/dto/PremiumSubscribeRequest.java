package com.desserting.backend.domain.premium.dto;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "프리미엄 패스 결제 요청 (모의 결제)")
public record PremiumSubscribeRequest(
        @Schema(description = "모의 결제 수단", example = "CARD")
        @NotNull(message = "결제 수단(paymentMethod)은 필수입니다.")
        MockPaymentMethod paymentMethod
) {
}
