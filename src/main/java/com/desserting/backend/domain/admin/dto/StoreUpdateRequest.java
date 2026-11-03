package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 정보 수정 요청 DTO")
public class StoreUpdateRequest {

    @Schema(description = "소속 테마거리 ID (선택)", example = "1")
    private Long themeStreetId;

    @Schema(description = "매장명", example = "카페 루시드 동성로본점")
    private String name;

    @Schema(description = "매장 도로명 주소", example = "대구 중구 동성로4길 94")
    private String address;

    @Schema(description = "위도 (Latitude)", example = "35.8687000")
    private BigDecimal latitude;

    @Schema(description = "경도 (Longitude)", example = "128.5982000")
    private BigDecimal longitude;

    @Schema(description = "영업 시간 안내", example = "매일 12:00 - 22:00")
    private String businessHours;

    @Schema(description = "매장 대표 연락처", example = "053-422-7020")
    private String contact;

    @Schema(description = "매장 대표 커버 이미지 URL", example = "https://images.desserting.com/stores/lucid_new.jpg")
    private String coverImageUrl;

    @Schema(description = "디저팅 제휴 혜택 안내", example = "디저팅 매칭 커플 전 메뉴 15% 할인")
    private String partnerDiscountNotice;
}
