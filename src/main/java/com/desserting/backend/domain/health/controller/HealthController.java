package com.desserting.backend.domain.health.controller;

import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "00. Health Check", description = "서버 상태 점검 API")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Operation(summary = "서버 헬스 체크", description = "서버 구동 상태 및 활성 프로파일을 반환합니다.")
    @GetMapping
    public ApiResponse<Map<String, Object>> checkHealth() {
        return ApiResponse.success("서버가 정상 동작 중입니다.", Map.of(
                "status", "UP",
                "profile", activeProfile,
                "serverTime", LocalDateTime.now()
        ));
    }
}
