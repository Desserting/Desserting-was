package com.desserting.backend.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문할 메뉴와 수량")
public record OrderItemRequest(
        @Schema(description = "메뉴 ID (예약한 매장의 메뉴)", example = "3")
        @NotNull(message = "메뉴(menuId)는 필수입니다.")
        Long menuId,

        @Schema(description = "수량 (1~20)", example = "2")
        @NotNull(message = "수량(quantity)은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        @Max(value = 20, message = "수량은 20개 이하여야 합니다.")
        Integer quantity
) {
}
