package com.desserting.backend.domain.user.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "선택 가능한 대표 아바타")
public class AvatarOptionResponse {

    @Schema(description = "대응 디저트 취향", example = "TEA_NON_COFFEE")
    private DessertType dessertType;

    @Schema(description = "취향 표시명", example = "밀크티/전통차")
    private String description;

    @Schema(description = "아바타 이미지 URL (PATCH /users/me/profile, POST /auth/signup 의 dessertAvatarUrl 에 그대로 사용)",
            example = "https://desserting-web.vercel.app/avatars/tea.png")
    private String avatarUrl;

    public static AvatarOptionResponse from(DessertType type) {
        return AvatarOptionResponse.builder()
                .dessertType(type)
                .description(type.getDescription())
                .avatarUrl(type.getDefaultAvatarUrl())
                .build();
    }
}
