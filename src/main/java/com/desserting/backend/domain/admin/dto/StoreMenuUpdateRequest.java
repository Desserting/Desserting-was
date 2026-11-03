package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 디저트 메뉴 수정 요청 DTO")
public class StoreMenuUpdateRequest {

    @Schema(description = "메뉴명", example = "제주 애플망고 빙수")
    private String name;

    @Schema(description = "가격 (원)", example = "21000")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @Schema(description = "메뉴 설명", example = "제주산 애플망고를 가득 담은 프리미엄 빙수")
    private String description;

    @Schema(description = "메뉴 이미지 URL", example = "https://images.desserting.com/menus/jeju_mango.jpg")
    private String imageUrl;

    @Schema(description = "대표 메뉴 여부", example = "true")
    private Boolean isRepresentative;
}
