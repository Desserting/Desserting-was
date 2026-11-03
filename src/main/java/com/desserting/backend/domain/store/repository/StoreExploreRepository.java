package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * 지도·매장 탐색 전용 조회 리포지토리 (4-5, 4-6).
 * StoreRepository(공용)를 수정하지 않기 위해 분리했다.
 */
public interface StoreExploreRepository extends Repository<Store, Long>, JpaSpecificationExecutor<Store> {

    /** 테마거리별 제휴매장 수 [themeStreetId, count] */
    @Query("""
            SELECT s.themeStreet.id, COUNT(s) FROM Store s
            WHERE s.isPartner = true AND s.themeStreet IS NOT NULL
            GROUP BY s.themeStreet.id
            """)
    List<Object[]> countPartnerStoresByThemeStreet();
}
