package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = {"themeStreet"})
    @Override
    Page<Store> findAll(Pageable pageable);

    List<Store> findAllByIsPartnerTrue();

    List<Store> findAllByThemeStreetIdAndIsPartnerTrue(Long themeStreetId);

    @Query("SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.menus WHERE s.id = :id")
    Optional<Store> findByIdWithMenus(@Param("id") Long id);
}
