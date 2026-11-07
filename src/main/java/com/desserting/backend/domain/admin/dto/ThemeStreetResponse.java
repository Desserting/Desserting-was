package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "테마거리 응답 DTO")
public class ThemeStreetResponse {

    @Schema(description = "테마거리 ID", example = "1")
    private Long id;

    @Schema(description = "테마거리 명칭", example = "앞산 카페거리")
    private String name;

    @Schema(description = "테마거리 소개", example = "대구 남구 대명동 일대 디저트 카페 거리")
    private String description;

    @Schema(description = "지도 점선 구역 GeoJSON 폴리곤 좌표")
    private String boundaryPolygonJson;

    @Schema(description = "중심 위도", example = "35.8324000")
    private BigDecimal centerLatitude;

    @Schema(description = "중심 경도", example = "128.5831000")
    private BigDecimal centerLongitude;

    @Schema(description = "대표 커버 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "등록 일시")
    private LocalDateTime createdAt;

    @Schema(description = "소속 제휴 매장 목록")
    private List<StoreSummary> stores;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreSummary {
        private Long storeId;
        private String name;
        private String address;
        private Boolean isPartner;
    }

    public static ThemeStreetResponse from(ThemeStreet themeStreet) {
        return from(themeStreet, Collections.emptyList());
    }

    public static ThemeStreetResponse from(ThemeStreet themeStreet, List<Store> stores) {
        List<StoreSummary> storeSummaries = stores != null ? stores.stream()
                .map(s -> StoreSummary.builder()
                        .storeId(s.getId())
                        .name(s.getName())
                        .address(s.getAddress())
                        .isPartner(s.getIsPartner())
                        .build())
                .toList() : Collections.emptyList();

        return ThemeStreetResponse.builder()
                .id(themeStreet.getId())
                .name(themeStreet.getName())
                .description(themeStreet.getDescription())
                .boundaryPolygonJson(themeStreet.getBoundaryPolygonJson())
                .centerLatitude(themeStreet.getCenterLatitude())
                .centerLongitude(themeStreet.getCenterLongitude())
                .coverImageUrl(themeStreet.getCoverImageUrl())
                .isActive(themeStreet.getIsActive())
                .createdAt(themeStreet.getCreatedAt())
                .stores(storeSummaries)
                .build();
    }
}
