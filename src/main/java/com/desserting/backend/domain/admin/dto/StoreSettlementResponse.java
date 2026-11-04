package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 7% 플랫폼 수수료 및 정산 집계 응답 DTO")
public class StoreSettlementResponse {

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "카페 루시드")
    private String storeName;

    @Schema(description = "방문 완료된 총 주문/예약 건수", example = "12")
    private Long completedOrderCount;

    @Schema(description = "방문 완료된 총 실결제 금액 (원, 쿠폰 할인 후)", example = "300000")
    private Long totalOrderAmount;

    @Schema(description = "플랫폼 수수료율 (%)", example = "7.0")
    @Builder.Default
    private Double platformFeeRate = 7.0;

    @Schema(description = "플랫폼 차감 수수료 (원, 7%)", example = "21000")
    private Long platformFeeAmount;

    @Schema(description = "점주 실수령 정산 지급액 (원, 93%)", example = "279000")
    private Long settlementAmount;

    @Schema(description = "월 구독료 정책 현황 (정가 20,000원 / 해커톤 런칭 프로모션)", example = "FREE_TRIAL_PROMOTION (3개월 무료 프로모션 적용 중)")
    private String monthlySubscriptionStatus;

    public static StoreSettlementResponse of(Long storeId, String storeName, Long completedCount, Long totalAmount) {
        long safeCount = (completedCount != null) ? completedCount : 0L;
        long safeTotal = (totalAmount != null) ? totalAmount : 0L;
        long fee = Math.round(safeTotal * 0.07);
        long settlement = safeTotal - fee;

        return StoreSettlementResponse.builder()
                .storeId(storeId)
                .storeName(storeName)
                .completedOrderCount(safeCount)
                .totalOrderAmount(safeTotal)
                .platformFeeRate(7.0)
                .platformFeeAmount(fee)
                .settlementAmount(settlement)
                .monthlySubscriptionStatus("FREE_TRIAL_PROMOTION (단디톤 런칭 기념 제휴 첫 3개월 월 20,000원 무료 면제)")
                .build();
    }
}
