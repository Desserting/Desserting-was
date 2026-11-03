package com.desserting.backend.domain.explore.error;

import java.time.LocalDateTime;
import java.util.List;

/** 공통 ErrorResponse 와 같은 JSON 형태 (success, code, message, errors, timestamp) */
public record ExploreErrorResponse(
        boolean success,
        String code,
        String message,
        List<Object> errors,
        LocalDateTime timestamp
) {
    public static ExploreErrorResponse of(String code, String message) {
        return new ExploreErrorResponse(false, code, message, List.of(), LocalDateTime.now());
    }
}
