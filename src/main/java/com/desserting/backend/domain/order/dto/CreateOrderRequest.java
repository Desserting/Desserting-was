package com.desserting.backend.domain.order.dto;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.order.entity.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "메뉴 주문 + 모의 결제 요청 (7-2)")
public record CreateOrderRequest(
        @Schema(description = "주문 메뉴 목록 (같은 메뉴는 수량이 합쳐집니다)")
        @NotEmpty(message = "메뉴를 1개 이상 선택해주세요.")
        @Size(max = 20, message = "메뉴는 최대 20종까지 주문할 수 있습니다.")
        List<@Valid OrderItemRequest> items,

        @Schema(description = "적용할 쿠폰 ID (생략하면 예약 시 고른 쿠폰 사용)", example = "1")
        Long couponId,

        @Schema(description = "모의 결제 수단", example = "CARD")
        @NotNull(message = "결제 수단(paymentMethod)은 필수입니다.")
        MockPaymentMethod paymentMethod,

        @Schema(description = "COMBINED 합산 결제 / SPLIT 방문 인원끼리 개별 결제", example = "COMBINED")
        @NotNull(message = "결제 방식(paymentType)은 필수입니다.")
        PaymentType paymentType
) {
}
