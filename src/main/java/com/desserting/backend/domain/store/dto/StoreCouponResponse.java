package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.store.entity.StoreCoupon;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "매장 할인쿠폰 정보 응답 DTO")
public record StoreCouponResponse(
        @Schema(description = "쿠폰 ID", example = "1")
        Long couponId,

        @Schema(description = "매장 ID", example = "3")
        Long storeId,

        @Schema(description = "매장명", example = "아눅 앞산")
        String storeName,

        @Schema(description = "쿠폰명", example = "단디톤 첫방문 2,000원 할인")
        String couponName,

        @Schema(description = "할인 금액 (원)", example = "2000")
        Integer discountAmount,

        @Schema(description = "최소 주문 금액 (원)", example = "10000")
        Integer minOrderAmount,

        @Schema(description = "활성화 여부", example = "true")
        Boolean isActive,

        @Schema(description = "만료 일자 (null이면 무기한)", example = "2026-12-31")
        LocalDate expiryDate
) {
    public static StoreCouponResponse from(StoreCoupon coupon) {
        return StoreCouponResponse.builder()
                .couponId(coupon.getId())
                .storeId(coupon.getStore().getId())
                .storeName(coupon.getStore().getName())
                .couponName(coupon.getCouponName())
                .discountAmount(coupon.getDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .isActive(coupon.getIsActive())
                .expiryDate(coupon.getExpiryDate())
                .build();
    }
}
