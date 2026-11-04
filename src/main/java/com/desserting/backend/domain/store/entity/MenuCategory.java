package com.desserting.backend.domain.store.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;

/** 매장 상세 메뉴 탭 분류 (D-06 메뉴 선택: 디저트 / 음료 / 세트) */
@Getter
@RequiredArgsConstructor
public enum MenuCategory {
    DESSERT("디저트"),
    DRINK("음료"),
    SET("세트");

    private final String label;

    private static final List<String> SET_KEYWORDS = List.of("세트", "set", "콤보", "combo");
    private static final List<String> DRINK_KEYWORDS = List.of(
            "커피", "라떼", "아메리카노", "에스프레소", "카푸치노", "모카", "마끼아또", "콜드브루", "핸드드립", "드립",
            "밀크티", "홍차", "녹차", "얼그레이", "캐모마일", "아이스티", "에이드", "주스", "스무디", "쉐이크", "음료",
            "coffee", "latte", "americano", "espresso", "tea", "ade", "juice", "smoothie");

    /**
     * 점주가 분류를 지정하지 않은 메뉴는 이름으로 추정한다.
     * '티라미수'처럼 '티'로 시작하는 디저트가 있어 '티' 한 글자는 음료 키워드로 쓰지 않는다.
     */
    public static MenuCategory infer(String menuName) {
        if (menuName == null) {
            return DESSERT;
        }
        String name = menuName.toLowerCase(Locale.ROOT);
        if (SET_KEYWORDS.stream().anyMatch(name::contains)) {
            return SET;
        }
        if (DRINK_KEYWORDS.stream().anyMatch(name::contains)) {
            return DRINK;
        }
        return DESSERT;
    }
}
