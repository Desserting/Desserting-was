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
@Schema(description = "매장 신규 등록 요청 DTO")
public class StoreCreateRequest {

    @Schema(description = "소속 테마거리 ID (선택)", example = "1")
    private Long themeStreetId;

    @Schema(description = "매장명", example = "카페 루시드")
    @NotBlank(message = "매장명은 필수입니다.")
    private String name;

    @Schema(description = "매장 도로명 주소", example = "대구 중구 동성로4길 94")
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Schema(description = "위도 (Latitude)", example = "35.8687000")
    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "33.0")
    @DecimalMax(value = "43.0")
    private BigDecimal latitude;

    @Schema(description = "경도 (Longitude)", example = "128.5982000")
    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "124.0")
    @DecimalMax(value = "132.0")
    private BigDecimal longitude;

    @Schema(description = "영업 시간 안내", example = "매일 12:00 - 21:30 (수요일 정기휴무)")
    private String businessHours;

    @Schema(description = "매장 대표 연락처", example = "053-422-7020")
    private String contact;

    @Schema(description = "매장 대표 커버 이미지 URL", example = "https://images.desserting.com/stores/lucid.jpg")
    private String coverImageUrl;

    @Schema(description = "매장 대표 커버 이미지 URL (호환용)", example = "https://images.desserting.com/stores/lucid.jpg")
    private String imageUrl;

    public String getEffectiveCoverImageUrl() {
        return (coverImageUrl != null && !coverImageUrl.isBlank()) ? coverImageUrl : imageUrl;
    }

    @Schema(description = "디저팅 제휴 혜택 안내", example = "디저팅 매칭 커플 전 메뉴 10% 할인")
    private String partnerDiscountNotice;

    @Schema(description = "즉시 제휴 승인 여부 (관리자 등록 시 기본 true)", example = "true")
    @Builder.Default
    private Boolean isPartner = true;

    @Schema(description = "매장 대표 디저트 태그 목록 (8종 중 다중 선택)")
    private java.util.List<com.desserting.backend.domain.user.entity.DessertType> dessertTypes;
}
