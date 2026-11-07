package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.domain.admin.dto.ThemeStreetCreateRequest;
import com.desserting.backend.domain.admin.dto.ThemeStreetResponse;
import com.desserting.backend.domain.admin.dto.ThemeStreetUpdateRequest;
import com.desserting.backend.domain.admin.service.AdminThemeService;
import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4-8. 테마거리 운영 및 구역 관리 API", description = "대구 테마거리 명칭, 소개, 지도 점선 구역 GeoJSON 폴리곤 경계 및 중심좌표 관리 (4-8)")
@RestController
@RequestMapping("/api/v1/admin/themes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminThemeController {

    private final AdminThemeService adminThemeService;

    @Operation(summary = "신규 테마거리 등록 (플랫폼 관리자, ADM-04)", description = "앞산 카페거리 등 신규 테마거리와 지도 점선 구역 GeoJSON 데이터를 등록합니다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThemeStreetResponse>> createThemeStreet(
            @Valid @RequestBody ThemeStreetCreateRequest request) {
        ThemeStreetResponse response = adminThemeService.createThemeStreet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("테마거리가 성공적으로 등록되었습니다.", response));
    }

    @Operation(summary = "테마거리 상세 조회 (관리자, ADM-04)", description = "특정 테마거리의 상세 정보와 폴리곤 좌표를 조회합니다.")
    @GetMapping("/{themeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<ThemeStreetResponse>> getThemeStreet(@PathVariable Long themeId) {
        ThemeStreetResponse response = adminThemeService.getThemeStreet(themeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "전체 테마거리 목록 조회 (관리자, ADM-04)", description = "등록된 전체 테마거리 목록을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<List<ThemeStreetResponse>>> getAllThemeStreets() {
        List<ThemeStreetResponse> responses = adminThemeService.getAllThemeStreets();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "테마거리 정보 수정 (플랫폼 관리자, ADM-04)", description = "테마거리 소개, 중심 좌표, 점선 폴리곤 GeoJSON 또는 활성화 여부를 수정합니다.")
    @PatchMapping("/{themeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThemeStreetResponse>> updateThemeStreet(
            @PathVariable Long themeId,
            @Valid @RequestBody ThemeStreetUpdateRequest request) {
        ThemeStreetResponse response = adminThemeService.updateThemeStreet(themeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "테마거리에 제휴매장 추가 (플랫폼 관리자, ADM-04)")
    @PostMapping("/{themeId}/stores/{storeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThemeStreetResponse>> assignStore(
            @PathVariable Long themeId,
            @PathVariable Long storeId) {
        ThemeStreetResponse response = adminThemeService.assignStoreToThemeStreet(themeId, storeId);
        return ResponseEntity.ok(ApiResponse.success("매장이 테마거리에 추가되었습니다.", response));
    }

    @Operation(summary = "테마거리에서 제휴매장 제거 (플랫폼 관리자, ADM-04)")
    @DeleteMapping("/{themeId}/stores/{storeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThemeStreetResponse>> removeStore(
            @PathVariable Long themeId,
            @PathVariable Long storeId) {
        ThemeStreetResponse response = adminThemeService.removeStoreFromThemeStreet(themeId, storeId);
        return ResponseEntity.ok(ApiResponse.success("매장이 테마거리에서 제거되었습니다.", response));
    }
}
