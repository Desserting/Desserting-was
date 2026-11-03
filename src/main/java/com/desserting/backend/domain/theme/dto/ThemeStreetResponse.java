package com.desserting.backend.domain.theme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "테마거리 (지도 점선 구역) 정보")
public class ThemeStreetResponse {

    @Schema(description = "테마거리 ID", example = "1")
    private Long themeStreetId;

    @Schema(description = "테마거리 이름", example = "앞산 카페거리")
    private String name;

    @Schema(description = "소개")
    private String description;

    @Schema(description = "구역 중심 좌표 (이름 라벨·확대 기준점)")
    private Coordinate center;

    @Schema(description = "점선 경계 폴리곤 꼭짓점 목록 (지도 SDK에 그대로 전달)")
    private List<Coordinate> boundary;

    @Schema(description = "대표 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "구역 내 제휴매장 수", example = "12")
    private long storeCount;
}
