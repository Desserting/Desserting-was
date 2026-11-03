package com.desserting.backend.domain.user.controller;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.dto.ProfileVisitorListResponse;
import com.desserting.backend.domain.user.dto.UpdateLocationRequest;
import com.desserting.backend.domain.user.dto.UpdateProfileRequest;
import com.desserting.backend.domain.user.dto.UserProfileResponse;
import com.desserting.backend.domain.user.service.UserService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4-1. 사용자 및 디저트 프로필 API", description = "내 프로필 조회/수정, 위치 갱신, 메이트 목록, 상대방 프로필 열람 API (4-1)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "전체 디저트 메이트 목록 조회 (탐색용, PRF-04)", description = "등록된 디저트 메이트 사용자 목록을 조회합니다. (로그인 시 본인 제외)")
    @GetMapping
    public ApiResponse<List<OtherUserProfileResponse>> getAllUsers(
            @Parameter(hidden = true) @CurrentUser(required = false) Long currentUserId) {
        List<OtherUserProfileResponse> responses = userService.getAllOtherUsers(currentUserId);
        return ApiResponse.success("디저트 메이트 목록 조회에 성공하였습니다.", responses);
    }

    @Operation(summary = "내 프로필 상세 조회 (PRF-01)", description = "로그인된 사용자의 프로필, 잔여 하트 수, 선호 디저트 태그 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@Parameter(hidden = true) @CurrentUser Long userId) {
        UserProfileResponse response = userService.getMyProfile(userId);
        return ApiResponse.success("내 프로필 조회에 성공하였습니다.", response);
    }

    @Operation(summary = "내 프로필 수정 (PRF-03)", description = "디저트 닉네임, 한 줄 자기소개, 활동 지역, 디저트 취향 태그 목록을 수정합니다.")
    @PatchMapping("/me/profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ApiResponse.success("프로필이 성공적으로 수정되었습니다.", response);
    }

    @Operation(summary = "내 위치 정보 업데이트 (LOC-01~03)", description = "GPS 좌표(위경도) 또는 수동 선택 지역을 갱신합니다.")
    @PatchMapping("/me/location")
    public ApiResponse<UserProfileResponse> updateLocation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody UpdateLocationRequest request) {
        UserProfileResponse response = userService.updateLocation(userId, request);
        return ApiResponse.success("위치 정보가 성공적으로 갱신되었습니다.", response);
    }

    @Operation(summary = "상대방 프로필 열람 (PRF-04)", description = "상대방의 디저트 닉네임, 아바타, 나이, 활동 지역, 디저트 취향을 무료로 열람합니다 (민감 정보 제외).")
    @GetMapping("/{userId}")
    public ApiResponse<OtherUserProfileResponse> getOtherUserProfile(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long userId) {
        OtherUserProfileResponse response = userService.getOtherUserProfile(currentUserId, userId);
        return ApiResponse.success("상대방 프로필 조회에 성공하였습니다.", response);
    }

    @Operation(summary = "내 프로필 방문자 목록 조회 (프리미엄 전용, PRF-05)",
            description = "나를 방문한 총 사용자 수와 최근 방문자 프로필 목록을 조회합니다. 일반 회원은 403 차단됩니다.")
    @GetMapping("/me/visitors")
    public ApiResponse<ProfileVisitorListResponse> getMyProfileVisitors(
            @Parameter(hidden = true) @CurrentUser Long userId) {
        ProfileVisitorListResponse response = userService.getProfileVisitors(userId);
        return ApiResponse.success("프로필 방문자 목록 조회에 성공하였습니다.", response);
    }
}
