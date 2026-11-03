package com.desserting.backend.domain.heart.dto;

import com.desserting.backend.domain.heart.entity.HeartPackage;
import com.desserting.backend.domain.heart.entity.HeartPurchase;
import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "하트 구매 결과")
public record HeartPurchaseResponse(
        @Schema(example = "1") Long purchaseId,
        @Schema(example = "HEARTS_5") HeartPackage packageCode,
        @Schema(description = "구매한 하트 수", example = "5") Integer quantity,
        @Schema(description = "하트 1개 가격(원)", example = "200") Integer unitPrice,
        @Schema(description = "결제 금액(원)", example = "1000") Integer amount,
        @Schema(example = "CARD") MockPaymentMethod paymentMethod,
        @Schema(description = "모의 결제 승인 번호", example = "MOCK-3f2a9c1e") String paymentKey,
        @Schema(description = "구매 후 보유 하트", example = "8") Integer heartCountAfter,
        LocalDateTime paidAt
) {
    public static HeartPurchaseResponse from(HeartPurchase p) {
        return new HeartPurchaseResponse(p.getId(), p.getHeartPackage(), p.getQuantity(), p.getUnitPrice(),
                p.getAmount(), p.getPaymentMethod(), p.getPaymentKey(), p.getHeartCountAfter(), p.getCreatedAt());
    }
}
