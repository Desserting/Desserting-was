package com.desserting.backend.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 합산 결제(한 명이 전액) / 개별 결제(방문 인원끼리 나눠 냄) */
@Getter
@RequiredArgsConstructor
public enum PaymentType {
    COMBINED("합산 결제"),
    SPLIT("개별 결제");

    private final String description;
}
