package com.desserting.backend.integration;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 프리미엄 테스트 유저(minjung@desserting.com) 로그인
        userToken = login("minjung@desserting.com", "test1234!");
    }

    private String login(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return jsonNode.get("data").get("accessToken").asText();
    }

    /** 시드 순서·DB 상태에 따라 id 가 달라지므로, 계정으로 로그인해 실제 id 를 조회한다 */
    private String userIdOf(String email, String password) throws Exception {
        MvcResult meResult = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + login(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(meResult.getResponse().getContentAsString())
                .get("data").get("userId").asText();
    }

    @Test
    @DisplayName("[통합 E2E] 받은 대화 요청 목록 조회 검증")
    void getReceivedRequests_Success() throws Exception {
        mockMvc.perform(get("/api/v1/matches/requests/received")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 프리미엄 회원 나를 좋아요한 사람 목록 조회 검증 (LIKE-02)")
    void getMyLikesReceived_PremiumSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/likes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 내 활성 채팅방 목록 및 MongoDB 메시지 커서 페이징 조회 검증 (CHT-01~02)")
    void chatRoomsAndMessages_Success() throws Exception {
        // 1. 내 활성 채팅방 목록 조회 (GET /api/v1/chats/rooms)
        MvcResult roomsResult = mockMvc.perform(get("/api/v1/chats/rooms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode roomsJson = objectMapper.readTree(roomsResult.getResponse().getContentAsString());
        assertThat(roomsJson.get("data").size()).isGreaterThanOrEqualTo(1);

        Long roomId = roomsJson.get("data").get(0).get("roomId").asLong();

        // 2. 해당 방의 MongoDB 이전 메시지 페이징 조회 (GET /api/v1/chats/rooms/{roomId}/messages)
        mockMvc.perform(get("/api/v1/chats/rooms/" + roomId + "/messages")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 유저 목록 탐색 API 조회 검증 (게스트 및 인증 유저)")
    void getAllUsers_Success() throws Exception {
        // 1. 게스트 조회 (인증 헤더 없음)
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 2. 로그인 유저 조회 (본인 제외)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 1:1 직통 채팅방 개설 및 재진입 조회 검증 (POST /api/v1/chats/rooms/direct)")
    void getOrCreateDirectRoom_Success() throws Exception {
        // 1. 운영자 계정(디저팅운영자)과의 1:1 대화방 개설 (id 는 고정값 대신 실제 조회)
        String adminUserId = userIdOf("admin@desserting.com", "admin1234!");
        MvcResult firstResult = mockMvc.perform(post("/api/v1/chats/rooms/direct")
                        .header("Authorization", "Bearer " + userToken)
                        .param("targetUserId", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").isNumber())
                .andExpect(jsonPath("$.data.otherUserNickname").value("디저팅운영자"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        Long roomId = jsonNode.get("data").get("roomId").asLong();

        // 2. 동일 대상 유저와 다시 호출 시 기존 방 반환
        mockMvc.perform(post("/api/v1/chats/rooms/direct")
                        .header("Authorization", "Bearer " + userToken)
                        .param("targetUserId", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(roomId))
                .andExpect(jsonPath("$.data.otherUserNickname").value("디저팅운영자"));
    }
}
