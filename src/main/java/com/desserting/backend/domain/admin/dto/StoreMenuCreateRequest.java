package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 디저트 메뉴 등록 요청 DTO")
public class StoreMenuCreateRequest {

    @Schema(description = "메뉴명", example = "망고 빙수")
    @NotBlank(message = "메뉴명은 필수입니다.")
    private String name;

    @Schema(description = "가격 (원)", example = "18000")
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @Schema(description = "메뉴 설명", example = "생망고가 듬뿍 올라간 시그니처 눈꽃빙수")
    private String description;

    @Schema(description = "메뉴 이미지 URL", example = "https://images.desserting.com/menus/mango_bingsoo.jpg")
    private String imageUrl;

    @Schema(description = "대표 메뉴 여부", example = "true")
    @Builder.Default
    private Boolean isRepresentative = false;
}
