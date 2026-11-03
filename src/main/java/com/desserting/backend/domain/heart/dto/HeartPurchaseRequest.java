package com.desserting.backend.domain.heart.dto;

import com.desserting.backend.domain.heart.entity.HeartPackage;
import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "하트 구매 요청 (모의 결제)")
public record HeartPurchaseRequest(
        @Schema(description = "상품 코드", example = "HEARTS_5")
        @NotNull(message = "구매할 상품(packageCode)은 필수입니다.")
        HeartPackage packageCode,

        @Schema(description = "모의 결제 수단", example = "CARD")
        @NotNull(message = "결제 수단(paymentMethod)은 필수입니다.")
        MockPaymentMethod paymentMethod
) {
}
