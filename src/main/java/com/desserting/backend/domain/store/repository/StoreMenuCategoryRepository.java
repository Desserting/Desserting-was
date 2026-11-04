package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreMenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoreMenuCategoryRepository extends JpaRepository<StoreMenuCategory, Long> {

    @Query("SELECT c FROM StoreMenuCategory c JOIN FETCH c.menu WHERE c.menu.id IN :menuIds")
    List<StoreMenuCategory> findAllByMenuIds(@Param("menuIds") Collection<Long> menuIds);

    Optional<StoreMenuCategory> findByMenuId(Long menuId);
}
