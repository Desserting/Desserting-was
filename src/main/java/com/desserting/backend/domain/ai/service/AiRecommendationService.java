package com.desserting.backend.domain.ai.service;

import com.desserting.backend.domain.ai.client.OllamaClient;
import com.desserting.backend.domain.ai.dto.AiRecommendationResponse;
import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.dto.ChatMessageRequest;
import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.service.ChatService;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreMenuRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
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
 * 채팅방의 최근 대화 내역 및 사용자 선호 디저트 취향을 분석하여 두 사람에게 가장 어울리는 제휴 카페를 추천하고,
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
    private final UserRepository userRepository;
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

        // 3. 최근 순수 대화 메시지만 수집 (최대 15건, 과거순 정렬 - 기존 AI 추천 카드는 제외하여 편향 방지)
        List<ChatMessageDocument> recentDocs = new ArrayList<>(
                chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 15))
                        .stream()
                        .filter(d -> !"PLACE_PROPOSAL".equalsIgnoreCase(d.getMessageType()))
                        .toList()
        );
        Collections.reverse(recentDocs);

        String chatDialogue = recentDocs.stream()
                .map(d -> String.format("%s: %s", d.getSenderNickname(), d.getContent()))
                .collect(Collectors.joining("\n"));

        // 4. 참여자 프로필 디저트 취향 태그 수집
        List<DessertType> preferredTypes = new ArrayList<>();
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser != null && currentUser.getDessertTags() != null) {
            currentUser.getDessertTags().forEach(t -> preferredTypes.add(t.getDessertType()));
        }
        Optional<ChatRoomMember> otherMemberOpt = chatRoomMemberRepository.findOtherMember(roomId, userId);
        if (otherMemberOpt.isPresent()) {
            User otherUser = otherMemberOpt.get().getUser();
            if (otherUser != null && otherUser.getDessertTags() != null) {
                otherUser.getDessertTags().forEach(t -> preferredTypes.add(t.getDessertType()));
            }
        }

        String tasteInfo = preferredTypes.isEmpty() ? "(선호 디저트 태그 없음)" :
                preferredTypes.stream().map(DessertType::name).distinct().collect(Collectors.joining(", "));

        // 5. 매장 및 대표 메뉴 정보 텍스트 구성 (N+1 쿼리 방지: IN 일괄 조회 후 메모리 매핑)
        List<Long> partnerStoreIds = partnerStores.stream().map(Store::getId).toList();
        Map<Long, List<StoreMenu>> menuMap = storeMenuRepository.findAllByStoreIdInAndIsSoldOutFalse(partnerStoreIds).stream()
                .filter(m -> m.getStore() != null)
                .collect(Collectors.groupingBy(m -> m.getStore().getId()));

        String candidateStoresText = partnerStores.stream()
                .map(s -> {
                    List<StoreMenu> menus = menuMap.getOrDefault(s.getId(), List.of());
                    String menuNames = menus.stream().map(StoreMenu::getName).collect(Collectors.joining(", "));
                    return String.format("[ID: %d] %s (%s) - 대표 메뉴: [%s]", s.getId(), s.getName(), s.getAddress(), menuNames);
                })
                .collect(Collectors.joining("\n"));

        // 6. Ollama Qwen 2.5 3B 호출 시도
        String systemPrompt = """
                너는 데이트 & 디저트 전문 AI 플래너야.
                두 사람의 최근 대화 내용과 디저트 선호 카테고리를 면밀히 분석하여, [추천 가능한 제휴 매장 목록] 중에서 가장 잘 어울리는 매장 1곳과 해당 매장의 대표 메뉴 1~2개를 골라줘.
                대화에서 특정 카페(루시드, 슬로우터틀, 카펠라, 딥커피, 아눅 등)나 디저트(빙수, 와플, 팬케이크, 에스프레소, 크루키 등)가 언급되었다면 반드시 그와 연관된 매장을 최우선으로 선정해야 해.
                반드시 아래 JSON 스키마를 엄격히 지켜서 오직 JSON 형식으로만 응답해:
                {
                  "storeId": (선택한 제휴 매장의 정수 ID),
                  "reason": "한국어로 작성된 친절하고 구체적인 추천 이유 한 문장",
                  "recommendedMenus": ["선택한 매장의 대표메뉴1", "선택한 매장의 대표메뉴2"]
                }
                """;

        String userPrompt = String.format("""
                [참여자 디저트 선호 카테고리]
                %s

                [최근 대화 내역]
                %s

                [추천 가능한 제휴 매장 목록]
                %s
                """, tasteInfo, chatDialogue.isBlank() ? "(대화가 아직 적습니다. 선호 카테고리나 대표 매장을 추천해주세요)" : chatDialogue, candidateStoresText);

        Optional<String> aiResponseOpt = ollamaClient.generate(systemPrompt, userPrompt);
        AiRecommendationResult result = aiResponseOpt
                .flatMap(raw -> parseAiResult(raw, partnerStores))
                .orElseGet(() -> smartFallback(roomId, chatDialogue, partnerStores, menuMap, preferredTypes));

        Store selectedStore = result.store();
        List<String> menus = result.menus();
        String reason = result.reason();
        String source = result.source();

        // 7. 채팅방에 PLACE_PROPOSAL 카드 메시지 자동 전송
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
            String cleanJson = jsonText.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode root = objectMapper.readTree(cleanJson);
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

    private AiRecommendationResult smartFallback(
            Long roomId,
            String dialogue,
            List<Store> stores,
            Map<Long, List<StoreMenu>> menuMap,
            List<DessertType> preferredTypes) {

        String normDialogue = (dialogue == null ? "" : dialogue).toLowerCase().replace(" ", "");

        // 1. 카페 핵심 이름 매칭 (루시드, 카펠라, 아눅, 슬로우터틀, 딥커피)
        for (Store store : stores) {
            String core = extractCoreStoreName(store.getName()).toLowerCase();
            String normFull = store.getName().toLowerCase().replace(" ", "");
            if ((!core.isBlank() && normDialogue.contains(core)) || normDialogue.contains(normFull)) {
                List<String> topMenus = menuMap.getOrDefault(store.getId(), List.of()).stream()
                        .map(StoreMenu::getName).limit(2).toList();
                String reason = String.format("두 분의 대화에서 언급된 %s의 대표 메뉴를 추천해 드립니다.", store.getName());
                return new AiRecommendationResult(store, topMenus, reason, "SMART_KEYWORD_FALLBACK");
            }
        }

        // 2. 공백 무시 메뉴명 매칭 (예: "푸딩빙수" -> "푸딩 빙수")
        for (Store store : stores) {
            List<StoreMenu> menus = menuMap.getOrDefault(store.getId(), List.of());
            for (StoreMenu menu : menus) {
                String normMenu = menu.getName().toLowerCase().replace(" ", "");
                if (normDialogue.contains(normMenu)) {
                    List<String> matched = List.of(menu.getName());
                    String reason = String.format("대화에서 나누신 '%s' 이야기를 바탕으로 %s을(를) 추천해 드립니다.", menu.getName(), store.getName());
                    return new AiRecommendationResult(store, matched, reason, "SMART_KEYWORD_FALLBACK");
                }
            }
        }

        // 3. 디저트 카테고리/지역 키워드 매칭
        for (Store store : stores) {
            List<String> tags = getStoreCategoryTags(store.getName());
            for (String tag : tags) {
                if (normDialogue.contains(tag.toLowerCase())) {
                    List<String> topMenus = menuMap.getOrDefault(store.getId(), List.of()).stream()
                            .map(StoreMenu::getName).limit(2).toList();
                    String reason = String.format("두 분이 관심 가지신 '%s' 디저트가 인기 있는 %s을(를) 추천해 드립니다.", tag, store.getName());
                    return new AiRecommendationResult(store, topMenus, reason, "SMART_KEYWORD_FALLBACK");
                }
            }
        }

        // 4. 참여자 프로필 디저트 취향 태그 매칭
        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            for (DessertType dt : preferredTypes) {
                for (Store store : stores) {
                    if (isStoreMatchingDessertType(store.getName(), dt)) {
                        List<String> topMenus = menuMap.getOrDefault(store.getId(), List.of()).stream()
                                .map(StoreMenu::getName).limit(2).toList();
                        String reason = String.format("두 분의 달콤한 '%s' 디저트 취향을 고려하여 %s을(를) 추천해 드립니다.", dt.getDescription(), store.getName());
                        return new AiRecommendationResult(store, topMenus, reason, "SMART_KEYWORD_FALLBACK");
                    }
                }
            }
        }

        // 5. 방 번호 기반 분산 추천 (다양성 보장 - 특정 카페 고정 방지)
        int index = (int) (Math.abs(roomId) % stores.size());
        Store fallbackStore = stores.get(index);
        List<String> defaultMenus = menuMap.getOrDefault(fallbackStore.getId(), List.of()).stream()
                .limit(2).map(StoreMenu::getName).toList();
        String reason = String.format("두 분의 새로운 만남을 달콤하게 밝혀줄 %s의 대표 메뉴를 추천해 드립니다.", fallbackStore.getName());
        return new AiRecommendationResult(fallbackStore, defaultMenus, reason, "SMART_KEYWORD_FALLBACK");
    }

    private String extractCoreStoreName(String rawName) {
        if (rawName.contains("루시드")) return "루시드";
        if (rawName.contains("카펠라")) return "카펠라";
        if (rawName.contains("아눅")) return "아눅";
        if (rawName.contains("슬로우터틀")) return "슬로우터틀";
        if (rawName.contains("딥커피")) return "딥커피";
        return "";
    }

    private List<String> getStoreCategoryTags(String storeName) {
        if (storeName.contains("루시드")) return List.of("빙수", "푸딩", "동성로");
        if (storeName.contains("카펠라")) return List.of("수플레", "팬케이크", "말차", "테린느", "앞산");
        if (storeName.contains("슬로우터틀")) return List.of("와플", "딸기", "케이크", "동성로");
        if (storeName.contains("딥커피")) return List.of("에스프레소", "쿠키", "바치디다마", "봉산");
        if (storeName.contains("아눅")) return List.of("치즈케이크", "크루키", "크로와상", "앞산", "베이커리");
        return List.of();
    }

    private boolean isStoreMatchingDessertType(String storeName, DessertType dt) {
        return switch (dt) {
            case ICE_CREAM_BINGSU -> storeName.contains("루시드");
            case CAKE, TART_PIE -> storeName.contains("슬로우터틀") || storeName.contains("카펠라");
            case BAKERY -> storeName.contains("아눅");
            case COFFEE, COOKIE_FINANCIER -> storeName.contains("딥커피") || storeName.contains("아눅");
            default -> false;
        };
    }

    private record AiRecommendationResult(Store store, List<String> menus, String reason, String source) {
    }
}
