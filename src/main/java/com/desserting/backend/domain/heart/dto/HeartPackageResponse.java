package com.desserting.backend.domain.heart.dto;

import com.desserting.backend.domain.heart.entity.HeartPackage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하트 구매 상품")
public record HeartPackageResponse(
        @Schema(description = "상품 코드", example = "HEARTS_5") HeartPackage packageCode,
        @Schema(description = "하트 수", example = "5") int quantity,
        @Schema(description = "하트 1개 가격(원)", example = "200") int unitPrice,
        @Schema(description = "결제 금액(원)", example = "1000") int price
) {
    public static HeartPackageResponse of(HeartPackage p, int unitPrice) {
        return new HeartPackageResponse(p, p.getQuantity(), unitPrice, p.priceOf(unitPrice));
    }
}
