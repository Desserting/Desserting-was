package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.store.entity.MenuCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 분류 지정 결과")
public record MenuCategoryResponse(
        @Schema(example = "3") Long menuId,
        @Schema(example = "DRINK") MenuCategory category,
        @Schema(example = "음료") String categoryLabel
) {
    public static MenuCategoryResponse of(Long menuId, MenuCategory category) {
        return new MenuCategoryResponse(menuId, category, category.getLabel());
    }
}
