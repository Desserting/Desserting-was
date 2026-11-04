package com.desserting.backend.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주문 금액 미리보기 (저장되지 않음)")
public record OrderPreviewResponse(
        @Schema(example = "12") Long reservationId,
        List<OrderItemResponse> items,
        @Schema(description = "메뉴 합계", example = "15000") Integer totalAmount,
        @Schema(description = "적용 쿠폰 ID (없으면 null)", example = "1") Long couponId,
        @Schema(description = "적용 쿠폰명", example = "디저팅 첫 데이트 2,000원 할인") String couponName,
        @Schema(description = "쿠폰 할인 금액", example = "2000") Integer discountAmount,
        @Schema(description = "실결제 금액", example = "13000") Integer finalAmount,
        @Schema(description = "개별 결제 시 나눌 인원 (예약 인원)", example = "2") Integer guestCount,
        @Schema(description = "개별 결제 시 1인당 금액 (앞사람부터 1원 단위 나머지 부담)", example = "[6500, 6500]")
        List<Integer> splitShareAmounts
) {
}
