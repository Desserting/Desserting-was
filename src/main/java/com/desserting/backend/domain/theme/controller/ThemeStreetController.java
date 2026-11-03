package com.desserting.backend.domain.theme.controller;

import com.desserting.backend.domain.theme.dto.ThemeStreetDetailResponse;
import com.desserting.backend.domain.theme.dto.ThemeStreetResponse;
import com.desserting.backend.domain.theme.service.ThemeStreetService;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "4-5. 테마거리 지도 API", description = "대구·경북 테마거리 점선 구역 및 구역 내 제휴매장 API (4-5)")
@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
public class ThemeStreetController {

    private final ThemeStreetService themeStreetService;

    @Operation(summary = "테마거리 목록 (MAP-01, MAP-02)",
            description = "지도 첫 화면에 그릴 테마거리 이름·중심좌표·점선 경계(boundary)·제휴매장 수를 반환합니다.")
    @GetMapping
    public ApiResponse<List<ThemeStreetResponse>> getThemeStreets() {
        return ApiResponse.success("테마거리 목록 조회에 성공하였습니다.", themeStreetService.getThemeStreets());
    }

    @Operation(summary = "테마거리 선택 (MAP-03~06)",
            description = "선택한 구역 정보와 구역 내 제휴매장(마커·목록 공용)을 반환합니다. dessertType으로 디저트 필터를 적용할 수 있습니다.")
    @GetMapping("/{themeStreetId}")
    public ApiResponse<ThemeStreetDetailResponse> getThemeStreetDetail(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long themeStreetId,
            @Parameter(description = "디저트 종류 필터") @RequestParam(required = false) DessertType dessertType,
            @Parameter(description = "거리 계산 기준 위도 (없으면 내 저장 위치)") @RequestParam(required = false) BigDecimal lat,
            @Parameter(description = "거리 계산 기준 경도 (없으면 내 저장 위치)") @RequestParam(required = false) BigDecimal lng) {
        return ApiResponse.success("테마거리 조회에 성공하였습니다.",
                themeStreetService.getThemeStreetDetail(userId, themeStreetId, dessertType, lat, lng));
    }
}
