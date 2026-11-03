package com.desserting.backend.domain.user.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "내 프로필 상세 정보 응답 DTO")
public class UserProfileResponse {

    @Schema(description = "사용자 고유 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "user@desserting.com")
    private String email;

    @Schema(description = "디저트 닉네임", example = "딸기쇼트케이크")
    private String nickname;

    @Schema(description = "성별", example = "FEMALE")
    private String gender;

    @Schema(description = "나이", example = "25")
    private Integer age;

    @Schema(description = "활동 지역", example = "대구 남구")
    private String region;

    @Schema(description = "위도", example = "35.839812")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "128.587632")
    private BigDecimal longitude;

    @Schema(description = "한 줄 자기소개", example = "주말에 앞산 카페거리 투어 같이 다닐 분 찾아요!")
    private String bio;

    @Schema(description = "보유 하트 수", example = "5")
    private Integer heartCount;

    @Schema(description = "프리미엄 패스 회원 여부", example = "false")
    private Boolean isPremium;

    @Schema(description = "디저트 대표 아바타 이미지 URL")
    private String dessertAvatarUrl;

    @Schema(description = "선호 디저트 취향 목록", example = "[\"CAKE\", \"COFFEE\"]")
    private List<DessertType> dessertTags;

    @Schema(description = "계정 생성 일시")
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        List<DessertType> tags = user.getDessertTags().stream()
                .map(UserDessertTag::getDessertType)
                .toList();

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .region(user.getRegion())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .bio(user.getBio())
                .heartCount(user.getHeartCount())
                .isPremium(user.getIsPremium())
                .dessertAvatarUrl(user.getDessertAvatarUrl())
                .dessertTags(tags)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
