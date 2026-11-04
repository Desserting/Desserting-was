package com.desserting.backend.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "주문 금액 미리보기 요청 (7-1 주문 확인 화면)")
public record OrderPreviewRequest(
        @Schema(description = "주문 메뉴 목록 (같은 메뉴는 수량이 합쳐집니다)")
        @NotEmpty(message = "메뉴를 1개 이상 선택해주세요.")
        @Size(max = 20, message = "메뉴는 최대 20종까지 주문할 수 있습니다.")
        List<@Valid OrderItemRequest> items,

        @Schema(description = "적용할 쿠폰 ID (생략하면 예약 시 고른 쿠폰 사용)", example = "1")
        Long couponId
) {
}
