package com.desserting.backend.domain.theme.service;

import com.desserting.backend.domain.store.dto.StoreCardResponse;
import com.desserting.backend.domain.store.dto.StoreSearchCondition;
import com.desserting.backend.domain.store.repository.StoreExploreRepository;
import com.desserting.backend.domain.store.service.StoreQueryService;
import com.desserting.backend.domain.theme.dto.Coordinate;
import com.desserting.backend.domain.theme.dto.ThemeStreetDetailResponse;
import com.desserting.backend.domain.theme.dto.ThemeStreetResponse;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 4-5. 제휴매장 지도·테마거리 (MAP-01~03, MAP-07)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeStreetService {

    private final ThemeStreetRepository themeStreetRepository;
    private final StoreExploreRepository storeExploreRepository;
    private final StoreQueryService storeQueryService;
    private final ThemeBoundaryParser boundaryParser;

    /** 지도 첫 화면: 활성 테마거리 전체의 점선 경계 + 매장 수 */
    public List<ThemeStreetResponse> getThemeStreets() {
        Map<Long, Long> storeCounts = partnerStoreCounts();
        return themeStreetRepository.findAllByIsActiveTrue().stream()
                .map(t -> toResponse(t, storeCounts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    /** 테마거리 선택: 구역 정보 + 구역 내 제휴매장 (디저트 필터 선택) */
    public ThemeStreetDetailResponse getThemeStreetDetail(Long userId, Long themeStreetId, DessertType dessertType,
                                                          BigDecimal lat, BigDecimal lng) {
        ThemeStreet themeStreet = themeStreetRepository.findById(themeStreetId)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));

        List<StoreCardResponse> stores = storeQueryService.searchStores(userId,
                StoreSearchCondition.ofThemeStreet(themeStreetId, dessertType, lat, lng));

        long totalCount = dessertType == null ? stores.size()
                : partnerStoreCounts().getOrDefault(themeStreetId, 0L);
        return new ThemeStreetDetailResponse(toResponse(themeStreet, totalCount), stores);
    }

    private Map<Long, Long> partnerStoreCounts() {
        return storeExploreRepository.countPartnerStoresByThemeStreet().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private ThemeStreetResponse toResponse(ThemeStreet t, long storeCount) {
        return ThemeStreetResponse.builder()
                .themeStreetId(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .center(new Coordinate(t.getCenterLatitude().doubleValue(), t.getCenterLongitude().doubleValue()))
                .boundary(boundaryParser.parse(t.getBoundaryPolygonJson()))
                .coverImageUrl(t.getCoverImageUrl())
                .storeCount(storeCount)
                .build();
    }
}
