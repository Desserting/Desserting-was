package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreMenu;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** 메뉴 분류 지정용 메뉴 조회. 공용 StoreMenuRepository(4-8)는 수정하지 않는다. */
public interface MenuLookupRepository extends Repository<StoreMenu, Long> {

    Optional<StoreMenu> findByIdAndStoreId(Long id, Long storeId);
}
