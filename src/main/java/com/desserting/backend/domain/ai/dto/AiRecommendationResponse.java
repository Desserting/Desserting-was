package com.desserting.backend.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "AI 채팅 분석 맞춤 매장/메뉴 추천 결과 DTO (AI-01)")
public record AiRecommendationResponse(
        @Schema(description = "채팅방 ID", example = "10")
        Long roomId,

        @Schema(description = "추천된 제휴 매장 ID", example = "1")
        Long storeId,

        @Schema(description = "매장명", example = "카페 루시드")
        String storeName,

        @Schema(description = "매장 주소", example = "대구 중구 동성로4길 94")
        String storeAddress,

        @Schema(description = "AI 추천 이유 및 한줄 분석", example = "두 분 모두 소금빵과 달콤한 디저트를 선호하셔서 대표 소금빵 맛집인 카페 루시드를 추천합니다.")
        String reason,

        @Schema(description = "AI 추천 메뉴 목록", example = "[\"카이막 소금빵\", \"아메리카노\"]")
        List<String> recommendedMenus,

        @Schema(description = "추천 생성 엔진 소스", example = "OLLAMA_LOCAL_QWEN")
        String source
) {
}
