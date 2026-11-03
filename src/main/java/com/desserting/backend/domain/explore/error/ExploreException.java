package com.desserting.backend.domain.explore.error;

import lombok.Getter;

@Getter
public class ExploreException extends RuntimeException {

    private final ExploreErrorCode errorCode;

    public ExploreException(ExploreErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ExploreException(ExploreErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
