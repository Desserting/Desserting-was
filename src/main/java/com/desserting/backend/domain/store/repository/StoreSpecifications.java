package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreDessertTag;
import com.desserting.backend.domain.user.entity.DessertType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * 지도·매장 목록 동적 필터 (MAP-03~06, SHOP-01).
 */
public final class StoreSpecifications {

    private StoreSpecifications() {
    }

    /** 제휴 계약된 매장만 노출 (SELL-01 정책) */
    public static Specification<Store> isPartner() {
        return (root, query, cb) -> cb.isTrue(root.get("isPartner"));
    }

    public static Specification<Store> inThemeStreet(Long themeStreetId) {
        return (root, query, cb) -> cb.equal(root.get("themeStreet").get("id"), themeStreetId);
    }

    /** 해당 디저트 종류 태그가 달린 매장 */
    public static Specification<Store> sellsDessert(DessertType dessertType) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<StoreDessertTag> tag = sub.from(StoreDessertTag.class);
            sub.select(tag.get("id"))
                    .where(cb.equal(tag.get("store"), root),
                            cb.equal(tag.get("dessertType"), dessertType));
            return cb.exists(sub);
        };
    }

    /** 주소 접두어 (예: "대구 남구") */
    public static Specification<Store> addressStartsWith(String prefix) {
        return (root, query, cb) -> cb.like(root.get("address"), prefix + "%");
    }

    /** 지도 화면 영역 (남서·북동 꼭짓점) */
    public static Specification<Store> withinBounds(BigDecimal swLat, BigDecimal swLng,
                                                    BigDecimal neLat, BigDecimal neLng) {
        return (root, query, cb) -> cb.and(
                cb.between(root.get("latitude"), swLat, neLat),
                cb.between(root.get("longitude"), swLng, neLng));
    }
}
