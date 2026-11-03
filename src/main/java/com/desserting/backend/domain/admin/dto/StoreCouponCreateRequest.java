package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "매장 자율 할인쿠폰 생성 요청 DTO (ADM-06)")
public record StoreCouponCreateRequest(
        @Schema(description = "쿠폰명", example = "앞산 카페거리 데이트 2,000원 할인")
        @NotBlank(message = "쿠폰명은 필수입니다.")
        @Size(max = 100, message = "쿠폰명은 100자 이하로 입력해주세요.")
        String couponName,

        @Schema(description = "할인 금액 (원)", example = "2000")
        @NotNull(message = "할인 금액은 필수입니다.")
        @Min(value = 500, message = "최소 할인 금액은 500원 이상이어야 합니다.")
        Integer discountAmount,

        @Schema(description = "최소 주문 금액 (원)", example = "10000")
        @NotNull(message = "최소 주문 금액은 필수입니다.")
        @Min(value = 0, message = "최소 주문 금액은 0원 이상이어야 합니다.")
        Integer minOrderAmount,

        @Schema(description = "만료 일자 (null이면 무기한)", example = "2026-12-31")
        LocalDate expiryDate
) {
}
