package com.desserting.backend.domain.order.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 합산 결제(한 명이 전액) / 개별 결제(방문 인원끼리 나눠 냄) */
@Getter
@RequiredArgsConstructor
public enum PaymentType {
    COMBINED("합산 결제"),
    SPLIT("개별 결제");

    private final String description;

    @JsonCreator
    public static PaymentType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        if ("INDIVIDUAL".equals(upper) || "SPLIT".equals(upper)) {
            return SPLIT;
        }
        if ("COMBINED".equals(upper)) {
            return COMBINED;
        }
        throw new IllegalArgumentException("Unknown paymentType: " + value + " (허용 값: COMBINED, SPLIT, INDIVIDUAL)");
    }
}
