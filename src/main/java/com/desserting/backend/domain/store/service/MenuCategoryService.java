package com.desserting.backend.domain.store.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.store.entity.MenuCategory;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.entity.StoreMenuCategory;
import com.desserting.backend.domain.store.repository.MenuLookupRepository;
import com.desserting.backend.domain.store.repository.StoreMenuCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 메뉴 분류(디저트/음료/세트) 조회·지정 */
@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final StoreMenuCategoryRepository categoryRepository;
    private final MenuLookupRepository menuLookupRepository;

    /** 메뉴별 분류. 점주가 지정한 값이 우선이고, 없으면 메뉴 이름으로 추정한다. */
    @Transactional(readOnly = true)
    public Map<Long, MenuCategory> resolve(Collection<StoreMenu> menus) {
        Map<Long, MenuCategory> result = new HashMap<>();
        if (menus.isEmpty()) {
            return result;
        }
        for (StoreMenu menu : menus) {
            result.put(menu.getId(), MenuCategory.infer(menu.getName()));
        }
        categoryRepository.findAllByMenuIds(result.keySet())
                .forEach(c -> result.put(c.getMenu().getId(), c.getCategory()));
        return result;
    }

    /** 점주·관리자가 메뉴 분류를 지정한다 */
    @Transactional
    public MenuCategory assign(Long storeId, Long menuId, MenuCategory category) {
        StoreMenu menu = menuLookupRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.MENU_NOT_FOUND));
        categoryRepository.findByMenuId(menuId)
                .ifPresentOrElse(c -> c.change(category),
                        () -> categoryRepository.save(StoreMenuCategory.of(menu, category)));
        return category;
    }
}
