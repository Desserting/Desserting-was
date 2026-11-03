package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.store.entity.StoreMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 디저트 메뉴 응답 DTO")
public class StoreMenuResponse {

    @Schema(description = "메뉴 ID", example = "1")
    private Long menuId;

    @Schema(description = "메뉴명", example = "망고 빙수")
    private String name;

    @Schema(description = "가격 (원)", example = "18000")
    private Integer price;

    @Schema(description = "메뉴 상세 설명", example = "생망고가 듬뿍 올라간 시그니처 눈꽃빙수")
    private String description;

    @Schema(description = "메뉴 이미지 URL")
    private String imageUrl;

    @Schema(description = "대표 메뉴 여부", example = "true")
    private Boolean isRepresentative;

    @Schema(description = "품절 여부", example = "false")
    private Boolean isSoldOut;

    public static StoreMenuResponse from(StoreMenu menu) {
        return StoreMenuResponse.builder()
                .menuId(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .description(menu.getDescription())
                .imageUrl(menu.getImageUrl())
                .isRepresentative(menu.getIsRepresentative())
                .isSoldOut(menu.getIsSoldOut())
                .build();
    }
}
