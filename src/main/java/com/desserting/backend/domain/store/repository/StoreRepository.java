package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

    List<Store> findAllByIsPartnerTrue();

    List<Store> findAllByThemeStreetIdAndIsPartnerTrue(Long themeStreetId);

    long countByThemeStreetIdAndIsPartnerTrue(Long themeStreetId);

    @Query("SELECT s FROM Store s LEFT JOIN FETCH s.menus WHERE s.id = :id")
    Optional<Store> findByIdWithMenus(@Param("id") Long id);

    /** 테마거리별 제휴매장 수 [themeStreetId, count] */
    @Query("""
            SELECT s.themeStreet.id, COUNT(s) FROM Store s
            WHERE s.isPartner = true AND s.themeStreet IS NOT NULL
            GROUP BY s.themeStreet.id
            """)
    List<Object[]> countPartnerStoresByThemeStreet();
}
