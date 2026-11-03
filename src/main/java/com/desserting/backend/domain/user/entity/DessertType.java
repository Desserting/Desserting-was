package com.desserting.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DessertType {
    CAKE("케이크", "https://assets.desserting.com/avatars/cake.png"),
    BAKERY("베이커리/식빵", "https://assets.desserting.com/avatars/bakery.png"),
    COOKIE_FINANCIER("구움과자/휘낭시에", "https://assets.desserting.com/avatars/cookie.png"),
    TART_PIE("타르트/파이", "https://assets.desserting.com/avatars/tart.png"),
    ICE_CREAM_BINGSU("빙수/아이스크림", "https://assets.desserting.com/avatars/bingsu.png"),
    COFFEE("핸드드립/스페셜티 커피", "https://assets.desserting.com/avatars/coffee.png"),
    TEA_NON_COFFEE("밀크티/전통차", "https://assets.desserting.com/avatars/tea.png"),
    TRADITIONAL_KOREAN("개성주악/전통 디저트", "https://assets.desserting.com/avatars/traditional.png");

    private final String description;
    private final String defaultAvatarUrl;
}
