package com.desserting.backend.domain.store.dto;

import com.desserting.backend.domain.store.entity.MenuCategory;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "제휴매장 상세 (SHOP-02, D-02)")
public class StoreDetailResponse {

    private Long storeId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    @Schema(description = "영업시간", example = "매일 11:00 - 22:00")
    private String businessHours;

    @Schema(description = "연락처", example = "053-123-4567")
    private String contact;

    private String coverImageUrl;

    @Schema(description = "제휴 할인 안내 (SHOP-03)")
    private String partnerDiscountNotice;

    private Long themeStreetId;
    private String themeStreetName;

    @Schema(description = "판매 디저트 종류", example = "[\"CAKE\", \"COFFEE\"]")
    private List<DessertType> dessertTypes;

    @Schema(description = "메뉴 목록 (대표 메뉴 우선)")
    private List<MenuResponse> menus;

    public static StoreDetailResponse of(Store store, List<DessertType> dessertTypes,
                                         Map<Long, MenuCategory> menuCategories) {
        return StoreDetailResponse.builder()
                .storeId(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .latitude(store.getLatitude().doubleValue())
                .longitude(store.getLongitude().doubleValue())
                .businessHours(store.getBusinessHours())
                .contact(store.getContact())
                .coverImageUrl(store.getCoverImageUrl())
                .partnerDiscountNotice(store.getPartnerDiscountNotice())
                .themeStreetId(store.getThemeStreet() != null ? store.getThemeStreet().getId() : null)
                .themeStreetName(store.getThemeStreet() != null ? store.getThemeStreet().getName() : null)
                .dessertTypes(dessertTypes)
                .menus(store.getMenus().stream()
                        .sorted(Comparator.comparing((StoreMenu m) -> !Boolean.TRUE.equals(m.getIsRepresentative()))
                                .thenComparing(StoreMenu::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(m -> MenuResponse.from(m, menuCategories.getOrDefault(m.getId(), MenuCategory.infer(m.getName()))))
                        .toList())
                .build();
    }

    @Schema(description = "매장 메뉴")
    public record MenuResponse(
            Long menuId,
            String name,
            @Schema(example = "7500") Integer price,
            String description,
            String imageUrl,
            boolean representative,
            boolean soldOut,
            @Schema(description = "메뉴 탭 분류 (DESSERT, DRINK, SET)", example = "DESSERT") MenuCategory category,
            @Schema(example = "디저트") String categoryLabel
    ) {
        static MenuResponse from(StoreMenu m, MenuCategory category) {
            return new MenuResponse(m.getId(), m.getName(), m.getPrice(), m.getDescription(), m.getImageUrl(),
                    Boolean.TRUE.equals(m.getIsRepresentative()), Boolean.TRUE.equals(m.getIsSoldOut()),
                    category, category.getLabel());
        }
    }
}
