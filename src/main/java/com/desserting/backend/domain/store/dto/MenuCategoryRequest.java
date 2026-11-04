package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.store.entity.MenuCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "메뉴 분류 지정 요청")
public record MenuCategoryRequest(
        @Schema(description = "메뉴 분류 (DESSERT, DRINK, SET)", example = "DRINK")
        @NotNull(message = "메뉴 분류(category)는 필수입니다.")
        MenuCategory category
) {
}
