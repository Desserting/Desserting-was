package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "테마거리 신규 등록 요청 DTO")
public class ThemeStreetCreateRequest {

    @Schema(description = "테마거리 명칭", example = "앞산 카페거리")
    @NotBlank(message = "테마거리 명칭은 필수입니다.")
    private String name;

    @Schema(description = "테마거리 소개", example = "대구 남구 대명동 일대 아름다운 조망과 스페셜티 디저트 카페 밀집 거리")
    private String description;

    @Schema(description = "지도 점선 구역 GeoJSON 폴리곤 좌표 문자열", example = "[[[128.580, 35.830], [128.590, 35.830], [128.590, 35.835], [128.580, 35.835], [128.580, 35.830]]]")
    private String boundaryPolygonJson;

    @Schema(description = "중심 위도", example = "35.8324000")
    @NotNull(message = "중심 위도는 필수입니다.")
    @DecimalMin(value = "33.0")
    @DecimalMax(value = "43.0")
    private BigDecimal centerLatitude;

    @Schema(description = "중심 경도", example = "128.5831000")
    @NotNull(message = "중심 경도는 필수입니다.")
    @DecimalMin(value = "124.0")
    @DecimalMax(value = "132.0")
    private BigDecimal centerLongitude;

    @Schema(description = "대표 커버 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "활성화 여부", example = "true")
    @Builder.Default
    private Boolean isActive = true;
}
