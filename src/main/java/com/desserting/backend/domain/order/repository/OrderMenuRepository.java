package com.desserting.backend.domain.order.repository;

import com.desserting.backend.domain.store.entity.StoreMenu;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

/** 4-7 주문용 메뉴 조회. 공용 StoreMenuRepository(4-8)는 수정하지 않는다. */
public interface OrderMenuRepository extends Repository<StoreMenu, Long> {

    List<StoreMenu> findAllByStoreIdAndIdIn(Long storeId, Collection<Long> ids);
}
