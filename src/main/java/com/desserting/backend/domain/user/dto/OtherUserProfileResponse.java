package com.desserting.backend.domain.user.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "상대방 프로필 열람 응답 DTO (민감정보 제외)")
public class OtherUserProfileResponse {

    @Schema(description = "상대방 사용자 ID", example = "2")
    private Long userId;

    @Schema(description = "디저트 닉네임", example = "초코크로와상")
    private String nickname;

    @Schema(description = "성별", example = "MALE")
    private String gender;

    @Schema(description = "나이", example = "27")
    private Integer age;

    @Schema(description = "활동 지역 (대구/경북 구·군)", example = "대구 수성구")
    private String region;

    @Schema(description = "한 줄 자기소개", example = "스페셜티 커피와 휘낭시에 좋아합니다.")
    private String bio;

    @Schema(description = "프리미엄 회원 여부", example = "false")
    private Boolean isPremium;

    @Schema(description = "디저트 대표 아바타 이미지 URL")
    private String dessertAvatarUrl;

    @Schema(description = "선호 디저트 취향 목록", example = "[\"BAKERY\", \"COFFEE\"]")
    private List<DessertType> dessertTags;

    @Schema(description = "내가 이 사용자를 좋아요(Like)했는지 여부", example = "true")
    private Boolean liked;

    public static OtherUserProfileResponse from(User user) {
        return from(user, null);
    }

    public static OtherUserProfileResponse from(User user, Boolean liked) {
        List<DessertType> tags = user.getDessertTags().stream()
                .map(UserDessertTag::getDessertType)
                .toList();

        return OtherUserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .region(user.getRegion())
                .bio(user.getBio())
                .isPremium(user.getIsPremium())
                .dessertAvatarUrl(user.getDessertAvatarUrl())
                .dessertTags(tags)
                .liked(liked)
                .build();
    }
}
