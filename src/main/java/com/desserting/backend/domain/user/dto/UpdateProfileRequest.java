package com.desserting.backend.domain.user.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "프로필 수정 요청 DTO")
public class UpdateProfileRequest {

    @Schema(description = "변경할 디저트 닉네임", example = "블루베리타르트")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
    private String nickname;

    @Schema(description = "한 줄 자기소개", example = "앞산 카페거리와 삼덕동 투어 환영해요!")
    @Size(max = 255, message = "자기소개는 255자 이하이어야 합니다.")
    private String bio;

    @Schema(description = "활동 지역", example = "대구 중구")
    private String region;

    @Schema(description = "대표 디저트 아바타 이미지 URL. GET /api/v1/users/avatars 목록의 avatarUrl만 허용(그 외 400 U004). 생략 시 기존 값 유지",
            example = "https://desserting-web.vercel.app/avatars/tea.png")
    private String dessertAvatarUrl;

    @Schema(description = "선호 디저트 취향 목록", example = "[\"TART_PIE\", \"TEA_NON_COFFEE\"]")
    private List<DessertType> dessertTypes;
}
