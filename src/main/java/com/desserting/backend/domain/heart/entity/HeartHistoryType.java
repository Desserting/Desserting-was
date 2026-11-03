package com.desserting.backend.domain.heart.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HeartHistoryType {
    DAILY_CHARGE("매일 무료 하트 충전"),
    REQUEST_DEDUCT("대화 요청 전송 차감"),
    REQUEST_REFUND("대화 요청 거절 환불"),
    PURCHASE("하트 추가 구매 충전"),
    ADMIN_GRANT("관리자 지급");

    private final String description;
}
