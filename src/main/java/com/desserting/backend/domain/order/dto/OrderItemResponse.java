package com.desserting.backend.domain.order.dto;

import com.desserting.backend.domain.order.entity.ReservationOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 메뉴 1줄")
public record OrderItemResponse(
        @Schema(example = "3") Long menuId,
        @Schema(example = "딸기 생크림 케이크") String menuName,
        @Schema(example = "7500") Integer unitPrice,
        @Schema(example = "2") Integer quantity,
        @Schema(example = "15000") Integer lineAmount
) {
    public static OrderItemResponse from(ReservationOrderItem item) {
        return new OrderItemResponse(item.getMenuId(), item.getMenuName(), item.getUnitPrice(),
                item.getQuantity(), item.getLineAmount());
    }
}
