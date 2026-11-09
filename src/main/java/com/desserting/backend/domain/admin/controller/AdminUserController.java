package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.domain.admin.dto.AdminUserResponse;
import com.desserting.backend.domain.admin.dto.HeartGrantRequest;
import com.desserting.backend.domain.admin.service.AdminUserService;
import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4-8. 관리자 사용자 및 재화(하트) 관리 API", description = "전체 사용자 목록 열람 및 심사용 하트 즉시 지급 관리")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final com.desserting.backend.domain.matching.service.MatchingConfigService matchingConfigService;

    @Operation(summary = "랜덤 매칭 더미 유저 활성화 여부 조회 (관리자)", description = "랜덤 매칭 시 더미 유저 포함 여부를 조회합니다.")
    @GetMapping("/dummy-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> getDummyMatchConfig() {
        boolean enabled = matchingConfigService.isDummyMatchEnabled();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("dummyMatchEnabled", enabled)));
    }

    @Operation(summary = "랜덤 매칭 더미 유저 활성화 여부 토글 (관리자)", description = "랜덤 매칭 시 더미 유저 포함 여부를 ON/OFF 토글합니다.")
    @PostMapping("/toggle-dummy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> toggleDummyMatch() {
        boolean nextState = matchingConfigService.toggleDummyMatch();
        return ResponseEntity.ok(ApiResponse.success("더미 유저 매칭 설정이 변경되었습니다.", java.util.Map.of("dummyMatchEnabled", nextState)));
    }

    @Operation(summary = "전체 사용자 목록 조회 (관리자, ADM-USER)", description = "등록된 전체 사용자(회원, 점주, 관리자) 목록과 보유 하트 수를 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {
        List<AdminUserResponse> responses = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "사용자 하트 즉시 충전 (관리자, ADM-HEART)", description = "특정 사용자에게 원하는 개수만큼 하트를 즉시 지급합니다.")
    @PostMapping("/{userId}/hearts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> grantHearts(
            @PathVariable Long userId,
            @Valid @RequestBody HeartGrantRequest request) {
        AdminUserResponse response = adminUserService.grantHearts(userId, request);
        return ResponseEntity.ok(ApiResponse.success("하트가 성공적으로 지급되었습니다.", response));
    }
}
