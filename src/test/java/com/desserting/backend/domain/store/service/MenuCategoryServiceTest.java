package com.desserting.backend.domain.store.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.store.entity.MenuCategory;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.entity.StoreMenuCategory;
import com.desserting.backend.domain.store.repository.MenuLookupRepository;
import com.desserting.backend.domain.store.repository.StoreMenuCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

    @Mock private StoreMenuCategoryRepository categoryRepository;
    @Mock private MenuLookupRepository menuLookupRepository;
    @InjectMocks private MenuCategoryService service;

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "바스크 치즈케이크, DESSERT", "티라미수, DESSERT", "푸딩 빙수, DESSERT",
            "아눅 크림 라떼, DRINK", "아이스 아메리카노, DRINK", "레몬 에이드, DRINK", "로얄 밀크티, DRINK", "Cold Brew Coffee, DRINK",
            "디저팅 커플 세트, SET", "케이크 + 커피 세트, SET"
    })
    @DisplayName("분류를 지정하지 않은 메뉴는 이름으로 추정한다")
    void infer(String name, MenuCategory expected) {
        assertThat(MenuCategory.infer(name)).isEqualTo(expected);
    }

    @Test
    @DisplayName("점주가 지정한 분류가 이름 추정보다 우선한다")
    void resolve_explicitWins() {
        StoreMenu latte = menu(1L, "크림 라떼");
        StoreMenu cake = menu(2L, "치즈케이크");
        given(categoryRepository.findAllByMenuIds(anyCollection()))
                .willReturn(List.of(StoreMenuCategory.of(latte, MenuCategory.SET)));

        Map<Long, MenuCategory> result = service.resolve(List.of(latte, cake));

        assertThat(result).containsEntry(1L, MenuCategory.SET).containsEntry(2L, MenuCategory.DESSERT);
    }

    @Test
    @DisplayName("분류 지정: 처음이면 저장하고, 있으면 바꾼다. 다른 매장 메뉴는 EX501")
    void assign() {
        StoreMenu cake = menu(2L, "치즈케이크");
        StoreMenuCategory existing = StoreMenuCategory.of(cake, MenuCategory.DESSERT);
        given(menuLookupRepository.findByIdAndStoreId(1L, 10L)).willReturn(Optional.of(menu(1L, "세트")));
        given(categoryRepository.findByMenuId(1L)).willReturn(Optional.empty());
        given(menuLookupRepository.findByIdAndStoreId(2L, 10L)).willReturn(Optional.of(cake));
        given(categoryRepository.findByMenuId(2L)).willReturn(Optional.of(existing));

        service.assign(10L, 1L, MenuCategory.DRINK);
        service.assign(10L, 2L, MenuCategory.SET);

        verify(categoryRepository).save(any(StoreMenuCategory.class));
        assertThat(existing.getCategory()).isEqualTo(MenuCategory.SET);
        assertThatThrownBy(() -> service.assign(99L, 1L, MenuCategory.DRINK))
                .isInstanceOf(ExploreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.MENU_NOT_FOUND);
    }

    private StoreMenu menu(Long id, String name) {
        StoreMenu m = StoreMenu.builder().name(name).price(1000).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }
}
