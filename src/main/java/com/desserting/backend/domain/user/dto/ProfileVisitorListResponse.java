package com.desserting.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "내 프로필 방문자 목록 및 총 방문수 응답 DTO (PRF-05)")
public record ProfileVisitorListResponse(
        @Schema(description = "총 방문자 수", example = "12")
        long totalVisitorCount,

        @Schema(description = "최근 방문자 목록 (최신순)")
        List<ProfileVisitorResponse> visitors
) {
    public static ProfileVisitorListResponse of(long totalCount, List<ProfileVisitorResponse> visitors) {
        return ProfileVisitorListResponse.builder()
                .totalVisitorCount(totalCount)
                .visitors(visitors)
                .build();
    }
}
