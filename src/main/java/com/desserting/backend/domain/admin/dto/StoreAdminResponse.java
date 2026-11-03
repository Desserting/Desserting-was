package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "관리자용 매장 상세 응답 DTO")
public class StoreAdminResponse {

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "소속 테마거리 ID")
    private Long themeStreetId;

    @Schema(description = "소속 테마거리명", example = "앞산 카페거리")
    private String themeStreetName;

    @Schema(description = "상호명", example = "카페 루시드")
    private String name;

    @Schema(description = "도로명 주소", example = "대구 중구 동성로4길 94")
    private String address;

    @Schema(description = "위도", example = "35.8687000")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "128.5982000")
    private BigDecimal longitude;

    @Schema(description = "영업 시간", example = "매일 12:00 - 21:30")
    private String businessHours;

    @Schema(description = "연락처", example = "053-422-7020")
    private String contact;

    @Schema(description = "커버 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "제휴 승인 여부", example = "true")
    private Boolean isPartner;

    @Schema(description = "제휴 할인 혜택 문구", example = "디저팅 매칭 커플 전 메뉴 10% 할인")
    private String partnerDiscountNotice;

    @Schema(description = "등록된 디저트 메뉴 목록")
    private List<StoreMenuResponse> menus;

    @Schema(description = "매장 등록 일시")
    private LocalDateTime createdAt;

    public static StoreAdminResponse from(Store store) {
        return StoreAdminResponse.builder()
                .storeId(store.getId())
                .themeStreetId(store.getThemeStreet() != null ? store.getThemeStreet().getId() : null)
                .themeStreetName(store.getThemeStreet() != null ? store.getThemeStreet().getName() : null)
                .name(store.getName())
                .address(store.getAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .businessHours(store.getBusinessHours())
                .contact(store.getContact())
                .coverImageUrl(store.getCoverImageUrl())
                .isPartner(store.getIsPartner())
                .partnerDiscountNotice(store.getPartnerDiscountNotice())
                .menus(store.getMenus() != null ? store.getMenus().stream().map(StoreMenuResponse::from).toList() : List.of())
                .createdAt(store.getCreatedAt())
                .build();
    }
}
