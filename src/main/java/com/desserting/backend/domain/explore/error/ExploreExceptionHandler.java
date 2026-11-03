package com.desserting.backend.domain.explore.error;

import com.desserting.backend.domain.matching.controller.MatchingController;
import com.desserting.backend.domain.reservation.controller.ReservationController;
import com.desserting.backend.domain.store.controller.StoreController;
import com.desserting.backend.domain.theme.controller.ThemeStreetController;
import com.desserting.backend.global.error.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 탐색 도메인 컨트롤러에만 적용되는 예외 처리기.
 * 공통 GlobalExceptionHandler 는 그대로 두고, 이 컨트롤러들에서만 먼저 처리한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        MatchingController.class, ThemeStreetController.class, StoreController.class, ReservationController.class})
public class ExploreExceptionHandler {

    @ExceptionHandler(ExploreException.class)
    public ResponseEntity<ExploreErrorResponse> handleExploreException(ExploreException e) {
        log.warn("ExploreException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ExploreErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    /** 공통 에러 코드(USER_NOT_FOUND 등)도 상세 메시지를 살려서 응답 */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExploreErrorResponse> handleCustomException(CustomException e) {
        log.warn("CustomException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ExploreErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ExploreErrorResponse> handleInvalidParameter(Exception e) {
        log.warn("Invalid request parameter: {}", e.getMessage());
        String detail = e instanceof MethodArgumentTypeMismatchException m
                ? "'" + m.getName() + "' 값이 올바르지 않습니다: " + m.getValue()
                : e.getMessage();
        ExploreErrorCode code = ExploreErrorCode.INVALID_PARAMETER;
        return ResponseEntity.status(code.getHttpStatus()).body(ExploreErrorResponse.of(code.getCode(), detail));
    }
}
