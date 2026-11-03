package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "테마거리 정보 수정 요청 DTO")
public class ThemeStreetUpdateRequest {

    @Schema(description = "테마거리 명칭", example = "앞산 카페거리")
    private String name;

    @Schema(description = "테마거리 소개")
    private String description;

    @Schema(description = "지도 점선 구역 GeoJSON 폴리곤 좌표 문자열")
    private String boundaryPolygonJson;

    @Schema(description = "중심 위도")
    private BigDecimal centerLatitude;

    @Schema(description = "중심 경도")
    private BigDecimal centerLongitude;

    @Schema(description = "대표 커버 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "활성화 여부")
    private Boolean isActive;
}
