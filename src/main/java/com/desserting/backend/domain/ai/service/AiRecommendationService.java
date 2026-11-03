package com.desserting.backend.domain.ai.service;

import com.desserting.backend.domain.ai.client.OllamaClient;
import com.desserting.backend.domain.ai.dto.AiRecommendationResponse;
import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.dto.ChatMessageRequest;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.service.ChatService;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreMenuRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 4-5. 로컬 LLM(qwen2.5:3b) 기반 맞춤 제휴 카페 & 디저트 메뉴 추천 서비스 (AI-01)
 * 채팅방의 최근 대화 내역을 분석하여 두 사람의 취향에 가장 어울리는 제휴 카페를 추천하고,
 * 채팅방에 PLACE_PROPOSAL 카드 메시지를 자동 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final ChatService chatService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiRecommendationResponse recommendForRoom(Long roomId, Long userId) {
        // 1. 채팅방 참여 권한 검증
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(roomId, userId)) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
        }

        // 2. 제휴 매장 목록 조회 (후보 풀)
        List<Store> partnerStores = storeRepository.findAllByIsPartnerTrue();
        if (partnerStores.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND, "현재 추천 가능한 제휴 매장이 없습니다.");
        }

        // 3. 최근 채팅 메시지 수집 (최대 15건, 과거순 정렬)
        List<ChatMessageDocument> recentDocs = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 15));
        Collections.reverse(recentDocs);

        String chatDialogue = recentDocs.stream()
                .map(d -> String.format("%s: %s", d.getSenderNickname(), d.getContent()))
                .collect(Collectors.joining("\n"));

        // 4. 매장 및 대표 메뉴 정보 텍스트 구성
        Map<Long, List<StoreMenu>> menuMap = partnerStores.stream()
                .collect(Collectors.toMap(
                        Store::getId,
                        s -> storeMenuRepository.findAllByStoreIdAndIsSoldOutFalse(s.getId())
                ));

        String candidateStoresText = partnerStores.stream()
                .map(s -> {
                    List<StoreMenu> menus = menuMap.getOrDefault(s.getId(), List.of());
                    String menuNames = menus.stream().map(StoreMenu::getName).collect(Collectors.joining(", "));
                    return String.format("[ID: %d] %s (%s) - 판매 메뉴: [%s]", s.getId(), s.getName(), s.getAddress(), menuNames);
                })
                .collect(Collectors.joining("\n"));

        // 5. Ollama Qwen 2.5 3B 호출 시도
        String systemPrompt = """
                너는 데이트 & 디저트 전문 AI 플래너야.
                주어진 두 사람의 대화 내용을 바탕으로, 후보 매장 목록 중 두 사람의 취향에 가장 어울리는 매장 1곳과 추천 메뉴 1~2개를 골라줘.
                반드시 아래 JSON 스키마를 엄격히 지켜서 오직 JSON 형식으로만 응답해:
                {
                  "storeId": 1,
                  "reason": "한국어로 작성된 친절한 추천 이유 한 문장",
                  "recommendedMenus": ["추천메뉴1", "추천메뉴2"]
                }
                """;

        String userPrompt = String.format("""
                [최근 대화 내역]
                %s

                [추천 가능한 제휴 매장 목록]
                %s
                """, chatDialogue.isBlank() ? "(대화가 아직 적습니다. 대표 디저트 맛집을 추천해주세요)" : chatDialogue, candidateStoresText);

        Optional<String> aiResponseOpt = ollamaClient.generate(systemPrompt, userPrompt);
        AiRecommendationResult result = aiResponseOpt
                .flatMap(raw -> parseAiResult(raw, partnerStores))
                .orElseGet(() -> smartFallback(chatDialogue, partnerStores, menuMap));

        Store selectedStore = result.store();
        List<String> menus = result.menus();
        String reason = result.reason();
        String source = result.source();

        // 6. 채팅방에 PLACE_PROPOSAL 카드 메시지 자동 전송
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("storeId", selectedStore.getId());
        metadata.put("storeName", selectedStore.getName());
        metadata.put("storeAddress", selectedStore.getAddress());
        metadata.put("recommendedMenus", menus);
        metadata.put("reason", reason);
        metadata.put("aiSource", source);

        String cardContent = String.format("🍰 [AI 데이트 제안] %s (%s) 어때요? %s",
                selectedStore.getName(), String.join(", ", menus), reason);

        ChatMessageRequest proposalRequest = ChatMessageRequest.builder()
                .roomId(roomId)
                .messageType("PLACE_PROPOSAL")
                .content(cardContent)
                .metadata(metadata)
                .build();

        try {
            chatService.sendMessage(userId, proposalRequest);
        } catch (Exception e) {
            log.warn("AI 추천 카드 메시지 채팅방 발행 중 예외 발생: {}", e.getMessage());
        }

        return AiRecommendationResponse.builder()
                .roomId(roomId)
                .storeId(selectedStore.getId())
                .storeName(selectedStore.getName())
                .storeAddress(selectedStore.getAddress())
                .reason(reason)
                .recommendedMenus(menus)
                .source(source)
                .build();
    }

    private Optional<AiRecommendationResult> parseAiResult(String jsonText, List<Store> stores) {
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            long storeId = root.path("storeId").asLong(0);
            String reason = root.path("reason").asText("");
            List<String> menus = new ArrayList<>();
            if (root.has("recommendedMenus") && root.get("recommendedMenus").isArray()) {
                root.get("recommendedMenus").forEach(m -> menus.add(m.asText()));
            }

            Optional<Store> storeOpt = stores.stream().filter(s -> s.getId().equals(storeId)).findFirst();
            if (storeOpt.isPresent() && !reason.isBlank()) {
                return Optional.of(new AiRecommendationResult(storeOpt.get(), menus, reason, "OLLAMA_LOCAL_QWEN"));
            }
        } catch (Exception e) {
            log.warn("Ollama 응답 JSON 파싱 실패: {}. 폴백으로 전환합니다.", e.getMessage());
        }
        return Optional.empty();
    }

    private AiRecommendationResult smartFallback(String dialogue, List<Store> stores, Map<Long, List<StoreMenu>> menuMap) {
        // 대화에서 키워드 추출
        String lower = dialogue.toLowerCase();
        Store bestStore = stores.get(0);
        List<String> matchedMenus = new ArrayList<>();

        for (Store store : stores) {
            List<StoreMenu> menus = menuMap.getOrDefault(store.getId(), List.of());
            for (StoreMenu menu : menus) {
                if (lower.contains(menu.getName().toLowerCase())) {
                    bestStore = store;
                    matchedMenus.add(menu.getName());
                    break;
                }
            }
            if (!matchedMenus.isEmpty()) {
                break;
            }
        }

        if (matchedMenus.isEmpty()) {
            List<StoreMenu> defaultMenus = menuMap.getOrDefault(bestStore.getId(), List.of());
            matchedMenus = defaultMenus.stream().limit(2).map(StoreMenu::getName).toList();
        }

        String reason = String.format("두 분의 대화 흐름과 취향을 고려하여 %s의 인기 메뉴를 추천해 드립니다.", bestStore.getName());
        return new AiRecommendationResult(bestStore, matchedMenus, reason, "SMART_KEYWORD_FALLBACK");
    }

    private record AiRecommendationResult(Store store, List<String> menus, String reason, String source) {
    }
}
