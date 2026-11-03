package com.desserting.backend.domain.theme.dto;

import com.desserting.backend.domain.store.dto.StoreCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "테마거리 선택 결과: 구역 정보 + 구역 내 제휴매장 목록 (MAP-03)")
public record ThemeStreetDetailResponse(
        @Schema(description = "테마거리 정보") ThemeStreetResponse themeStreet,
        @Schema(description = "구역 내 제휴매장 (지도 마커·하단 목록 공용)") List<StoreCardResponse> stores
) {
}
