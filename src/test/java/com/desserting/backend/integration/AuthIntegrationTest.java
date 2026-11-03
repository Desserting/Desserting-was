package com.desserting.backend.integration;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.desserting.backend.domain.auth.dto.SignUpRequest;
import com.desserting.backend.domain.user.entity.DessertType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("[통합 E2E] 신규 회원가입 -> 로그인 -> 내 프로필 조회 전체 흐름 검증")
    void authAndProfileFlow_Success() throws Exception {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        String testEmail = "testuser_" + randomSuffix + "@desserting.com";
        String testNickname = "디저트" + randomSuffix;

        // 1. 회원가입 (POST /api/v1/auth/signup)
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .email(testEmail)
                .password("Password123!")
                .nickname(testNickname)
                .gender("FEMALE")
                .age(24)
                .region("대구 중구")
                .agreeToTerms(true)
                .dessertTypes(List.of(DessertType.BAKERY, DessertType.ICE_CREAM_BINGSU))
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 2. 로그인 (POST /api/v1/auth/login)
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        // 3. 내 프로필 조회 (GET /api/v1/users/me with Bearer token)
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testEmail))
                .andExpect(jsonPath("$.data.nickname").value(testNickname))
                .andExpect(jsonPath("$.data.heartCount").value(5)) // 신규 가입 무료 하트 5개 기본 지급
                .andExpect(jsonPath("$.data.dessertTags").isArray());

        // 4. 신규 가입 웰컴 하트 내역 조회 (GET /api/v1/hearts/history)
        mockMvc.perform(get("/api/v1/hearts/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("WELCOME_BONUS"))
                .andExpect(jsonPath("$.data[0].amount").value(5))
                .andExpect(jsonPath("$.data[0].balanceAfter").value(5));
    }

    @Test
    @DisplayName("[통합 E2E] 인증 토큰 없이 보호된 프로필 API 접근 시 401 차단 검증")
    void getMyProfile_Unauthenticated_Throws401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
