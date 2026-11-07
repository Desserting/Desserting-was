package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "관리자용 사용자 목록 및 하트 잔여량 응답 DTO")
public class AdminUserResponse {

    @Schema(description = "유저 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@desserting.com")
    private String email;

    @Schema(description = "닉네임", example = "딸기수플레")
    private String nickname;

    @Schema(description = "성별", example = "FEMALE")
    private String gender;

    @Schema(description = "나이", example = "25")
    private Integer age;

    @Schema(description = "활동 지역", example = "대구 중구")
    private String region;

    @Schema(description = "보유 하트 개수", example = "15")
    private Integer heartCount;

    @Schema(description = "프리미엄 구독 여부", example = "true")
    private Boolean isPremium;

    @Schema(description = "권한", example = "ROLE_USER")
    private String role;

    @Schema(description = "아바타 이미지 URL")
    private String dessertAvatarUrl;

    @Schema(description = "선호 디저트 태그 목록")
    private List<DessertType> dessertTypes;

    @Schema(description = "가입 일시")
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .region(user.getRegion())
                .heartCount(user.getHeartCount())
                .isPremium(Boolean.TRUE.equals(user.getIsPremium()))
                .role(user.getRole() != null ? user.getRole().name() : "ROLE_USER")
                .dessertAvatarUrl(user.getDessertAvatarUrl())
                .dessertTypes(user.getDessertTags().stream()
                        .map(UserDessertTag::getDessertType)
                        .toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
