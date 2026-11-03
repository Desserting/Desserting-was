package com.desserting.backend.domain.matching.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchType {
    SAME_TASTE("같은 취향 랜덤 (MAT-01)"),
    DIFFERENT_TASTE("다른 취향 랜덤 (MAT-02)");

    private final String description;
}
