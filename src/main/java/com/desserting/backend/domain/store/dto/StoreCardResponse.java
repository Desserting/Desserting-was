package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "제휴매장 카드 (지도 마커 + 목록 공용, MAP-04/05)")
public class StoreCardResponse {

    @Schema(description = "매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "매장명", example = "카페 올리브")
    private String name;

    @Schema(description = "도로명 주소", example = "대구 남구 앞산순환로 123")
    private String address;

    @Schema(description = "위도 (마커 위치)", example = "35.8297")
    private double latitude;

    @Schema(description = "경도 (마커 위치)", example = "128.5871")
    private double longitude;

    @Schema(description = "소속 테마거리 ID (없으면 null)", example = "1")
    private Long themeStreetId;

    @Schema(description = "소속 테마거리 이름", example = "앞산 카페거리")
    private String themeStreetName;

    @Schema(description = "대표 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "판매 디저트 종류", example = "[\"CAKE\", \"COFFEE\"]")
    private List<DessertType> dessertTypes;

    @Schema(description = "대표 메뉴명", example = "딸기 생크림 케이크")
    private String representativeMenuName;

    @Schema(description = "제휴 할인 안내 (SHOP-03)", example = "디저팅 회원 전 메뉴 10% 할인")
    private String partnerDiscountNotice;

    @Schema(description = "기준 위치로부터의 거리(km, 소수 첫째 자리). 기준 위치가 없으면 null", example = "0.8")
    private Double distanceKm;

    public static StoreCardResponse of(Store store, Double distanceKm) {
        List<StoreMenu> menus = store.getMenus();
        return StoreCardResponse.builder()
                .storeId(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .latitude(store.getLatitude().doubleValue())
                .longitude(store.getLongitude().doubleValue())
                .themeStreetId(store.getThemeStreet() != null ? store.getThemeStreet().getId() : null)
                .themeStreetName(store.getThemeStreet() != null ? store.getThemeStreet().getName() : null)
                .coverImageUrl(store.getCoverImageUrl())
                .dessertTypes(menus.stream().map(StoreMenu::getDessertType).filter(Objects::nonNull).distinct().toList())
                .representativeMenuName(menus.stream()
                        .filter(m -> Boolean.TRUE.equals(m.getIsRepresentative()))
                        .map(StoreMenu::getName)
                        .findFirst().orElse(null))
                .partnerDiscountNotice(store.getPartnerDiscountNotice())
                .distanceKm(distanceKm)
                .build();
    }
}
