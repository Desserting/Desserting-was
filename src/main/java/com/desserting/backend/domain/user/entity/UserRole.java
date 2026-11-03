package com.desserting.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ROLE_USER("일반 사용자"),
    ROLE_STORE_OWNER("제휴 매장 점주"),
    ROLE_ADMIN("플랫폼 관리자");

    private final String description;
}
