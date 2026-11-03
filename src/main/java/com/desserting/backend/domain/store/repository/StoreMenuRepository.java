package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreMenuRepository extends JpaRepository<StoreMenu, Long> {
    List<StoreMenu> findAllByStoreIdOrderByIdAsc(Long storeId);
    List<StoreMenu> findAllByStoreIdAndIsSoldOutFalse(Long storeId);
}
