package com.desserting.backend.domain.user.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.ProfileVisit;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "프로필 방문자 정보 응답 DTO")
public record ProfileVisitorResponse(
        @Schema(description = "방문자 회원 ID", example = "3")
        Long visitorId,

        @Schema(description = "방문자 닉네임", example = "딸기케이크")
        String nickname,

        @Schema(description = "방문자 프로필 아바타 이미지 URL", example = "https://desserting-web.vercel.app/avatars/cake.png")
        String profileImageUrl,

        @Schema(description = "방문자 활동 지역", example = "대구 중구")
        String region,

        @Schema(description = "방문자 선호 디저트 태그 목록")
        List<DessertType> preferredDesserts,

        @Schema(description = "최근 방문 일시", example = "2026-11-04T00:15:00")
        LocalDateTime visitedAt
) {
    public static ProfileVisitorResponse from(ProfileVisit visit) {
        User visitor = visit.getVisitor();
        List<DessertType> tags = visitor.getDessertTags().stream()
                .map(UserDessertTag::getDessertType)
                .toList();

        return ProfileVisitorResponse.builder()
                .visitorId(visitor.getId())
                .nickname(visitor.getNickname())
                .profileImageUrl(visitor.getDessertAvatarUrl())
                .region(visitor.getRegion())
                .preferredDesserts(tags)
                .visitedAt(visit.getVisitedAt())
                .build();
    }
}
