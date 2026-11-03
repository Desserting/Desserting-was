package com.desserting.backend.domain.store.service;

import com.desserting.backend.domain.store.dto.StoreCardResponse;
import com.desserting.backend.domain.store.dto.StoreDetailResponse;
import com.desserting.backend.domain.store.dto.StoreSearchCondition;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreSpecifications;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 4-5 지도 마커·목록 / 4-6 매장 탐색 조회 (MAP-03~06, MAP-08, SHOP-01~03)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public List<StoreCardResponse> searchStores(Long userId, StoreSearchCondition cond) {
        Specification<Store> spec = StoreSpecifications.isPartner();

        if (cond.themeStreetId() != null) {
            spec = spec.and(StoreSpecifications.inThemeStreet(cond.themeStreetId()));
        }
        if (cond.dessertType() != null) {
            spec = spec.and(StoreSpecifications.sellsDessert(cond.dessertType()));
        }
        if (cond.region() != null && !cond.region().isBlank()) {
            spec = spec.and(StoreSpecifications.addressStartsWith(cond.region().trim()));
        }
        if (cond.hasAnyBound()) {
            if (!cond.hasAllBounds()
                    || cond.swLat().compareTo(cond.neLat()) > 0
                    || cond.swLng().compareTo(cond.neLng()) > 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                        "지도 영역은 swLat ≤ neLat, swLng ≤ neLng 인 네 값을 모두 보내야 합니다.");
            }
            spec = spec.and(StoreSpecifications.withinBounds(cond.swLat(), cond.swLng(), cond.neLat(), cond.neLng()));
        }

        BigDecimal[] origin = resolveOrigin(userId, cond.lat(), cond.lng());
        List<StoreCardResponse> cards = storeRepository.findAll(spec).stream()
                .map(store -> StoreCardResponse.of(store, distanceFrom(origin, store)))
                .toList();

        Comparator<StoreCardResponse> order = origin != null
                ? Comparator.comparing(StoreCardResponse::getDistanceKm)
                : Comparator.comparing(StoreCardResponse::getName);
        return cards.stream().sorted(order).toList();
    }

    public StoreDetailResponse getStoreDetail(Long storeId) {
        Store store = storeRepository.findByIdWithMenus(storeId)
                .filter(s -> Boolean.TRUE.equals(s.getIsPartner()))
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        return StoreDetailResponse.from(store);
    }

    /** 거리 기준점: 요청 좌표 → 내 저장 위치 → 없음 */
    private BigDecimal[] resolveOrigin(Long userId, BigDecimal lat, BigDecimal lng) {
        if (lat != null && lng != null) {
            return new BigDecimal[]{lat, lng};
        }
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .filter(u -> u.getLatitude() != null && u.getLongitude() != null)
                .map(u -> new BigDecimal[]{u.getLatitude(), u.getLongitude()})
                .orElse(null);
    }

    private static Double distanceFrom(BigDecimal[] origin, Store store) {
        if (origin == null) {
            return null;
        }
        return GeoUtils.round1(GeoUtils.distanceKm(origin[0], origin[1], store.getLatitude(), store.getLongitude()));
    }
}
