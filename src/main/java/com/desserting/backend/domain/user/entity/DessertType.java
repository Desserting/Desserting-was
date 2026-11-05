package com.desserting.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DessertType {
    CAKE("케이크", "cake.png"),
    BAKERY("베이커리/식빵", "bakery.png"),
    COOKIE_FINANCIER("구움과자/휘낭시에", "cookie.png"),
    TART_PIE("타르트/파이", "tart.png"),
    ICE_CREAM_BINGSU("빙수/아이스크림", "bingsu.png"),
    COFFEE("핸드드립/스페셜티 커피", "coffee.png"),
    TEA_NON_COFFEE("밀크티/전통차", "tea.png"),
    TRADITIONAL_KOREAN("개성주악/전통 디저트", "traditional.png");

    /** 아바타 이미지는 프론트엔드(Vercel) public/avatars/ 에서 정적 서빙한다. */
    public static final String AVATAR_BASE_URL = "https://desserting-web.vercel.app/avatars/";
    /** 과거에 저장된(접근 불가) 아바타 URL prefix — 기동 시 AVATAR_BASE_URL 로 치환한다. */
    public static final String LEGACY_AVATAR_BASE_URL = "https://assets.desserting.com/avatars/";

    private final String description;
    private final String avatarFileName;

    public String getDefaultAvatarUrl() {
        return AVATAR_BASE_URL + avatarFileName;
    }

    /** 기본 제공 아바타 8종 중 하나인지 확인한다. 임의 외부 URL은 허용하지 않는다. */
    public static boolean isSelectableAvatarUrl(String url) {
        return url != null && Arrays.stream(values()).anyMatch(t -> t.getDefaultAvatarUrl().equals(url));
    }
}
