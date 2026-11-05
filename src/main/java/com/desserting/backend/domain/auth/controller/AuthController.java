package com.desserting.backend.domain.auth.controller;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.desserting.backend.domain.auth.dto.RefreshTokenRequest;
import com.desserting.backend.domain.auth.dto.SignUpRequest;
import com.desserting.backend.domain.auth.dto.TokenResponse;
import com.desserting.backend.domain.auth.service.AuthService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4-1. 계정 인증 및 토큰 API", description = "회원가입, 로그인, 로그아웃, JWT Access/Refresh 토큰 발급 및 갱신 API (4-1)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입 (ACC-01, ACC-02, PRF-01, PRF-02)",
            description = "이메일, 비밀번호, 닉네임, 성별, 나이(만 19세 이상), 활동 지역, 선호 디저트 취향을 등록하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        TokenResponse response = authService.signUp(request);
        return ApiResponse.success("회원가입이 완료되었습니다.", response);
    }

    @Operation(summary = "로그인 (ACC-01)", description = "이메일과 비밀번호로 로그인하여 Access/Refresh Token을 발급받습니다.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ApiResponse.success("로그인에 성공하였습니다.", response);
    }

    @Operation(summary = "토큰 재발급 (ACC-03)", description = "만료된 Access Token을 Refresh Token을 통해 재발급받습니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ApiResponse.success("토큰이 성공적으로 재발급되었습니다.", response);
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 Refresh Token을 무효화하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser Long userId) {
        authService.logout(userId);
        return ApiResponse.success("성공적으로 로그아웃되었습니다.", null);
    }
}
