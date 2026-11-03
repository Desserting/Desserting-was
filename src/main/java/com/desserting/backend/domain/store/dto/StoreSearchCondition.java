package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 지도·매장 목록 검색 조건 (모두 선택값).
 */
@Schema(description = "제휴매장 검색 조건")
public record StoreSearchCondition(
        @Schema(description = "테마거리 ID (구역 선택 시, MAP-03)", example = "1") Long themeStreetId,
        @Schema(description = "디저트 종류 필터 (MAP-06, SHOP-01)", example = "CAKE") DessertType dessertType,
        @Schema(description = "주소 접두어 지역 필터", example = "대구 남구") String region,
        @Schema(description = "지도 영역 남서쪽 위도") BigDecimal swLat,
        @Schema(description = "지도 영역 남서쪽 경도") BigDecimal swLng,
        @Schema(description = "지도 영역 북동쪽 위도") BigDecimal neLat,
        @Schema(description = "지도 영역 북동쪽 경도") BigDecimal neLng,
        @Schema(description = "거리 계산 기준 위도 (없으면 내 저장 위치)", example = "35.8290") BigDecimal lat,
        @Schema(description = "거리 계산 기준 경도 (없으면 내 저장 위치)", example = "128.5880") BigDecimal lng
) {

    public static StoreSearchCondition ofThemeStreet(Long themeStreetId, DessertType dessertType,
                                                     BigDecimal lat, BigDecimal lng) {
        return new StoreSearchCondition(themeStreetId, dessertType, null, null, null, null, null, lat, lng);
    }

    public boolean hasAnyBound() {
        return swLat != null || swLng != null || neLat != null || neLng != null;
    }

    public boolean hasAllBounds() {
        return swLat != null && swLng != null && neLat != null && neLng != null;
    }
}
