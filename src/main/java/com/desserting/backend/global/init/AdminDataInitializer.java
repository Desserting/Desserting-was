package com.desserting.backend.global.init;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.entity.ChatRoom;
import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomRepository;
import com.desserting.backend.domain.matching.entity.ChatRequest;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import com.desserting.backend.domain.matching.repository.ChatRequestRepository;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreMenuRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreReservationSlotRepository;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.entity.UserLike;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.domain.user.repository.UserLikeRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ThemeStreetRepository themeStreetRepository;
    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            entityManager.createNativeQuery("ALTER TABLE heart_histories DROP CONSTRAINT IF EXISTS heart_histories_type_check").executeUpdate();
        } catch (Exception e) {
            log.debug("heart_histories_type_check 제약조건 해제 생략: {}", e.getMessage());
        }

        // 접근 불가한 구 아바타 호스트(assets.desserting.com)로 저장된 URL을 Vercel 경로로 치환
        int migrated = entityManager.createNativeQuery(
                        "UPDATE users SET dessert_avatar_url = REPLACE(dessert_avatar_url, :legacy, :current) "
                                + "WHERE dessert_avatar_url LIKE :legacyPattern")
                .setParameter("legacy", DessertType.LEGACY_AVATAR_BASE_URL)
                .setParameter("legacyPattern", DessertType.LEGACY_AVATAR_BASE_URL + "%")
                .setParameter("current", DessertType.AVATAR_BASE_URL)
                .executeUpdate();
        if (migrated > 0) {
            log.info("아바타 URL {}건을 Vercel 경로로 갱신했습니다.", migrated);
        }

        if (userRepository.existsByEmail("minjung@desserting.com")) {
            log.info("종합 목업 데이터가 이미 존재합니다. 시딩을 건너뜁니다.");
            return;
        }

        log.info("🚀 [단디톤 2026] 전 도메인 실감형 종합 목업 데이터 시딩을 시작합니다...");

        // =========================================================================
        // 1. 유저 계정 및 디저트 프로필 (총 8명: 관리자 1, 점주 2, 데이팅 유저 5)
        // =========================================================================
        User adminUser = User.builder()
                .email("admin@desserting.com")
                .password(passwordEncoder.encode("admin1234!"))
                .nickname("디저팅운영자")
                .gender("MALE")
                .age(30)
                .region("대구 중구")
                .bio("디저팅 플랫폼 공식 운영자 계정입니다.")
                .role(UserRole.ROLE_ADMIN)
                .isPremium(true)
                .heartCount(999)
                .dessertAvatarUrl(DessertType.COFFEE.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        adminUser.getDessertTags().add(UserDessertTag.builder().user(adminUser).dessertType(DessertType.COFFEE).build());
        userRepository.save(adminUser);

        User ownerLucid = User.builder()
                .email("owner@lucid.com")
                .password(passwordEncoder.encode("owner1234!"))
                .nickname("루시드점주")
                .gender("FEMALE")
                .age(35)
                .region("대구 중구")
                .bio("동성로 카페 루시드 대표입니다. 푸딩빙수와 함께 달콤한 데이트를 선물합니다.")
                .role(UserRole.ROLE_STORE_OWNER)
                .isPremium(false)
                .heartCount(10)
                .dessertAvatarUrl(DessertType.ICE_CREAM_BINGSU.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        ownerLucid.getDessertTags().add(UserDessertTag.builder().user(ownerLucid).dessertType(DessertType.ICE_CREAM_BINGSU).build());
        userRepository.save(ownerLucid);

        User ownerAnook = User.builder()
                .email("owner@anook.com")
                .password(passwordEncoder.encode("owner1234!"))
                .nickname("아눅점주")
                .gender("MALE")
                .age(38)
                .region("대구 남구")
                .bio("앞산 뷰와 크루키가 맛있는 아눅 앞산입니다.")
                .role(UserRole.ROLE_STORE_OWNER)
                .isPremium(false)
                .heartCount(10)
                .dessertAvatarUrl(DessertType.BAKERY.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        ownerAnook.getDessertTags().add(UserDessertTag.builder().user(ownerAnook).dessertType(DessertType.BAKERY).build());
        userRepository.save(ownerAnook);

        // 데이팅/매칭 일반 유저 5명
        User userMinjung = User.builder()
                .email("minjung@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("초코크로와상")
                .gender("FEMALE")
                .age(26)
                .region("대구 수성구")
                .bio("주말마다 대구 감성 베이커리 투어 다녀요. 갓 구운 휘낭시에와 크루키 사랑합니다 🥐")
                .role(UserRole.ROLE_USER)
                .isPremium(true) // 프리미엄 회원 -> 나를 좋아요한 사람 열람 가능
                .heartCount(50)
                .dessertAvatarUrl(DessertType.BAKERY.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        userMinjung.getDessertTags().add(UserDessertTag.builder().user(userMinjung).dessertType(DessertType.BAKERY).build());
        userMinjung.getDessertTags().add(UserDessertTag.builder().user(userMinjung).dessertType(DessertType.COOKIE_FINANCIER).build());
        userMinjung.getDessertTags().add(UserDessertTag.builder().user(userMinjung).dessertType(DessertType.ICE_CREAM_BINGSU).build());
        userRepository.save(userMinjung);

        User userJihun = User.builder()
                .email("jihun@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("드립마니아")
                .gender("MALE")
                .age(28)
                .region("대구 중구")
                .bio("에티오피아 예가체프 핸드드립과 진한 초코케이크 조합 좋아합니다 ☕")
                .role(UserRole.ROLE_USER)
                .isPremium(false)
                .heartCount(5)
                .dessertAvatarUrl(DessertType.COFFEE.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        userJihun.getDessertTags().add(UserDessertTag.builder().user(userJihun).dessertType(DessertType.COFFEE).build());
        userJihun.getDessertTags().add(UserDessertTag.builder().user(userJihun).dessertType(DessertType.CAKE).build());
        userRepository.save(userJihun);

        User userSoyeon = User.builder()
                .email("soyeon@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("딸기푸딩")
                .gender("FEMALE")
                .age(24)
                .region("대구 남구")
                .bio("달콤한 일본식 수제 푸딩과 시원한 밀크티 한 잔의 여유를 즐겨요 🍮")
                .role(UserRole.ROLE_USER)
                .isPremium(false)
                .heartCount(3)
                .dessertAvatarUrl(DessertType.ICE_CREAM_BINGSU.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        userSoyeon.getDessertTags().add(UserDessertTag.builder().user(userSoyeon).dessertType(DessertType.ICE_CREAM_BINGSU).build());
        userSoyeon.getDessertTags().add(UserDessertTag.builder().user(userSoyeon).dessertType(DessertType.TEA_NON_COFFEE).build());
        userRepository.save(userSoyeon);

        User userTaehyun = User.builder()
                .email("taehyun@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("타르트장인")
                .gender("MALE")
                .age(29)
                .region("대구 달서구")
                .bio("제철 생과일 타르트와 전통 개성주악 탐방이 취미입니다 🥧")
                .role(UserRole.ROLE_USER)
                .isPremium(true)
                .heartCount(20)
                .dessertAvatarUrl(DessertType.TART_PIE.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        userTaehyun.getDessertTags().add(UserDessertTag.builder().user(userTaehyun).dessertType(DessertType.TART_PIE).build());
        userTaehyun.getDessertTags().add(UserDessertTag.builder().user(userTaehyun).dessertType(DessertType.TRADITIONAL_KOREAN).build());
        userRepository.save(userTaehyun);

        User userYuna = User.builder()
                .email("yuna@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("치즈케이크러버")
                .gender("FEMALE")
                .age(27)
                .region("대구 북구")
                .bio("고소한 바스크 치즈케이크와 티타임 함께할 좋은 인연 찾아요 🍰")
                .role(UserRole.ROLE_USER)
                .isPremium(false)
                .heartCount(4)
                .dessertAvatarUrl(DessertType.CAKE.getDefaultAvatarUrl())
                .dessertTags(new ArrayList<>())
                .build();
        userYuna.getDessertTags().add(UserDessertTag.builder().user(userYuna).dessertType(DessertType.CAKE).build());
        userYuna.getDessertTags().add(UserDessertTag.builder().user(userYuna).dessertType(DessertType.COOKIE_FINANCIER).build());
        userRepository.save(userYuna);

        // =========================================================================
        // 2. 소셜 인터랙션 (좋아요 UserLike)
        // =========================================================================
        // 프리미엄 회원 minjung에게 jihun과 taehyun이 좋아요를 누른 상태
        userLikeRepository.save(UserLike.of(userJihun, userMinjung));
        userLikeRepository.save(UserLike.of(userTaehyun, userMinjung));
        // 상호 좋아요
        userLikeRepository.save(UserLike.of(userMinjung, userTaehyun));

        // =========================================================================
        // 3. 대화 요청 (ChatRequest: PENDING, ACCEPTED, REJECTED)
        // =========================================================================
        // jihun -> minjung : PENDING (minjung 받은요청함에서 확인 및 수락/거절 가능)
        ChatRequest pendingReq = ChatRequest.create(userJihun, userMinjung);
        chatRequestRepository.save(pendingReq);

        // taehyun -> soyeon : ACCEPTED
        ChatRequest acceptedReq = ChatRequest.builder()
                .sender(userTaehyun)
                .receiver(userSoyeon)
                .status(RequestStatus.ACCEPTED)
                .expiredAt(LocalDateTime.now().plusHours(20))
                .build();
        chatRequestRepository.save(acceptedReq);

        // yuna -> jihun : REJECTED
        ChatRequest rejectedReq = ChatRequest.builder()
                .sender(userYuna)
                .receiver(userJihun)
                .status(RequestStatus.REJECTED)
                .expiredAt(LocalDateTime.now().minusHours(2))
                .build();
        chatRequestRepository.save(rejectedReq);

        // =========================================================================
        // 4. 활성 채팅방 및 MongoDB 실시간 메시지 적재 (PLACE_PROPOSAL 매장 제안 포함)
        // =========================================================================
        // 채팅방 1: minjung 💖 taehyun (취향 일치 커플 데이트 제안 대화방)
        ChatRoom room1 = ChatRoom.create();
        room1.updateLastMessage("좋아요! 토요일 3시에 아눅 앞산에서 봬요 ㅎㅎ");
        ChatRoom savedRoom1 = chatRoomRepository.save(room1);

        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom1, userMinjung));
        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom1, userTaehyun));

        // MongoDB 채팅 메시지 6건 적재
        Long r1Id = savedRoom1.getId();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(3);

        List<ChatMessageDocument> r1Messages = List.of(
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userTaehyun.getId()).senderNickname(userTaehyun.getNickname())
                        .messageType("TEXT").content("안녕하세요 민정님! 디저트 프로필 보고 취향이 너무 잘 맞아서 연락드렸어요 ㅎㅎ")
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime).build(),
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userMinjung.getId()).senderNickname(userMinjung.getNickname())
                        .messageType("TEXT").content("안녕하세요 태현님! 반가워요~ 베이커리랑 구움과자 정말 좋아하시나 봐요!")
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime.plusMinutes(5)).build(),
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userTaehyun.getId()).senderNickname(userTaehyun.getNickname())
                        .messageType("TEXT").content("네! 특히 앞산 쪽에 크루키랑 바스크 치즈케이크 진짜 잘하는 뷰 맛집이 있거든요.")
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime.plusMinutes(10)).build(),
                // 🍰 핵심 스펙: 카페 방문 제안 메시지 (PLACE_PROPOSAL)
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userTaehyun.getId()).senderNickname(userTaehyun.getNickname())
                        .messageType("PLACE_PROPOSAL").content("이번 주 토요일에 앞산 카페거리 '아눅 앞산'에서 크루키 드시러 갈래요?")
                        .metadata(Map.of(
                                "storeId", 1,
                                "storeName", "아눅 앞산",
                                "proposedDate", "2026-11-08",
                                "proposedTime", "15:00",
                                "discountNotice", "데이트 예약 방문 시 시그니처 아메리카노 1잔 무료 증정"
                        ))
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime.plusMinutes(15)).build(),
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userMinjung.getId()).senderNickname(userMinjung.getNickname())
                        .messageType("TEXT").content("와 아눅 앞산 저 가보고 싶었던 곳인데 너무 좋아요! 토요일 3시 딱 좋아요 ㅎㅎ")
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime.plusMinutes(20)).build(),
                ChatMessageDocument.builder()
                        .roomId(r1Id).senderId(userTaehyun.getId()).senderNickname(userTaehyun.getNickname())
                        .messageType("TEXT").content("좋아요! 토요일 3시에 아눅 앞산에서 봬요 ㅎㅎ")
                        .readUserIds(new ArrayList<>(List.of(userTaehyun.getId(), userMinjung.getId())))
                        .createdAt(baseTime.plusMinutes(25)).build()
        );
        chatMessageRepository.saveAll(r1Messages);

        // 채팅방 2: soyeon 💖 jihun (동성로 푸딩빙수 토크 대화방)
        ChatRoom room2 = ChatRoom.create();
        room2.updateLastMessage("루시드 푸딩빙수 진짜 인생 빙수예요!");
        ChatRoom savedRoom2 = chatRoomRepository.save(room2);

        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom2, userSoyeon));
        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom2, userJihun));

        Long r2Id = savedRoom2.getId();
        List<ChatMessageDocument> r2Messages = List.of(
                ChatMessageDocument.builder()
                        .roomId(r2Id).senderId(userJihun.getId()).senderNickname(userJihun.getNickname())
                        .messageType("TEXT").content("소연님 안녕하세요! 동성로 쪽 푸딩빙수 드셔보셨나요?")
                        .readUserIds(new ArrayList<>(List.of(userJihun.getId(), userSoyeon.getId())))
                        .createdAt(baseTime.plusMinutes(30)).build(),
                ChatMessageDocument.builder()
                        .roomId(r2Id).senderId(userSoyeon.getId()).senderNickname(userSoyeon.getNickname())
                        .messageType("TEXT").content("안녕하세요 지훈님! 루시드 푸딩빙수 진짜 인생 빙수예요!")
                        .readUserIds(new ArrayList<>(List.of(userJihun.getId(), userSoyeon.getId())))
                        .createdAt(baseTime.plusMinutes(35)).build()
        );
        chatMessageRepository.saveAll(r2Messages);

        // =========================================================================
        // 5. 테마거리 3선 (앞산, 동성로, 김광석길)
        // =========================================================================
        ThemeStreet apsan = ThemeStreet.builder()
                .name("앞산 카페거리")
                .description("대구 남구 대명동 일대 수려한 앞산 전망과 프리미엄 스페셜티 베이커리 카페 거리")
                .boundaryPolygonJson("{\"type\":\"Polygon\",\"coordinates\":[[[128.5801,35.8302],[128.5915,35.8302],[128.5915,35.8385],[128.5801,35.8385],[128.5801,35.8302]]]}")
                .centerLatitude(new BigDecimal("35.8324000"))
                .centerLongitude(new BigDecimal("128.5831000"))
                .coverImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24")
                .isActive(true)
                .build();
        themeStreetRepository.save(apsan);

        ThemeStreet dongseongro = ThemeStreet.builder()
                .name("동성로 통신골목 카페거리")
                .description("대구 중심 번화가 동성로의 푸딩, 빙수 및 트렌디한 디저트 핫플레이스")
                .boundaryPolygonJson("{\"type\":\"Polygon\",\"coordinates\":[[[128.5950,35.8650],[128.6020,35.8650],[128.6020,35.8710],[128.5950,35.8710],[128.5950,35.8650]]]}")
                .centerLatitude(new BigDecimal("35.8687000"))
                .centerLongitude(new BigDecimal("128.5982000"))
                .coverImageUrl("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb")
                .isActive(true)
                .build();
        themeStreetRepository.save(dongseongro);

        ThemeStreet kimGwangseok = ThemeStreet.builder()
                .name("김광석 다시그리기길 카페거리")
                .description("대구 중구 대봉동 감성 골목과 스페셜티 에스프레소 바 밀집 지역")
                .boundaryPolygonJson("{\"type\":\"Polygon\",\"coordinates\":[[[128.6040,35.8600],[128.6100,35.8600],[128.6100,35.8650],[128.6040,35.8650],[128.6040,35.8600]]]}")
                .centerLatitude(new BigDecimal("35.8615000"))
                .centerLongitude(new BigDecimal("128.6068000"))
                .coverImageUrl("https://images.unsplash.com/photo-1442512595331-e89e73853f31")
                .isActive(true)
                .build();
        themeStreetRepository.save(kimGwangseok);

        // =========================================================================
        // 6. 대구 대표 제휴 디저트 카페 5선 (Store & StoreMenu)
        // =========================================================================
        // 1) 아눅 앞산
        Store anook = Store.builder()
                .themeStreet(apsan)
                .name("아눅 앞산 (a.nook Apsan)")
                .address("대구 남구 앞산순환로 459")
                .latitude(new BigDecimal("35.8324000"))
                .longitude(new BigDecimal("128.5831000"))
                .businessHours("매일 10:00 - 22:00")
                .contact("053-623-1060")
                .coverImageUrl("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085")
                .isPartner(true)
                .partnerDiscountNotice("데이트 예약 방문 시 시그니처 아메리카노 1잔 무료 증정")
                .build();
        storeRepository.save(anook);

        List<StoreMenu> anookMenus = List.of(
                StoreMenu.builder().store(anook).name("바스크 치즈케이크").price(8000).description("스페인 전통 방식으로 고온에서 진하게 구워낸 글루텐프리 치즈케이크").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(anook).name("피스타치오 크루키").price(6800).description("바삭한 크루아상과 진한 피스타치오 쿠키 도우의 환상적 조화").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(anook).name("아눅 크림 라떼").price(6500).description("달콤하고 묵직한 수제 생크림이 올라간 시그니처 커피").isRepresentative(false).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(anookMenus);

        // 2) 카펠라 (앞산 뷰 대형 카페)
        Store capella = Store.builder()
                .themeStreet(apsan)
                .name("카펠라 (Capella)")
                .address("대구 남구 앞산순환로 449")
                .latitude(new BigDecimal("35.8329000"))
                .longitude(new BigDecimal("128.5824000"))
                .businessHours("매일 11:00 - 23:00")
                .contact("053-652-3377")
                .coverImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24")
                .isPartner(true)
                .partnerDiscountNotice("디저팅 커플 예약 시 창가 뷰 석 우선 배정 & 5% 할인")
                .build();
        storeRepository.save(capella);

        List<StoreMenu> capellaMenus = List.of(
                StoreMenu.builder().store(capella).name("딸기 수플레 팬케이크").price(16000).description("입안에서 부드럽게 녹아내리는 폭신한 수플레와 생딸기").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(capella).name("말차 테린느").price(7000).description("제주 유기농 말차를 아낌없이 넣은 꾸덕한 테린느").isRepresentative(true).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(capellaMenus);

        // 3) 카페 루시드
        Store lucid = Store.builder()
                .themeStreet(dongseongro)
                .name("카페 루시드 (Cafe Lucid)")
                .address("대구 중구 동성로4길 94")
                .latitude(new BigDecimal("35.8687000"))
                .longitude(new BigDecimal("128.5982000"))
                .businessHours("매일 12:00 - 21:30 (수요일 정기휴무)")
                .contact("053-422-7020")
                .coverImageUrl("https://images.unsplash.com/photo-1559925393-8be0ec4767c8")
                .isPartner(true)
                .partnerDiscountNotice("디저팅 매칭 커플 전 메뉴 10% 할인")
                .build();
        storeRepository.save(lucid);

        List<StoreMenu> lucidMenus = List.of(
                StoreMenu.builder().store(lucid).name("푸딩 빙수").price(18000).description("부드러운 커스터드 푸딩과 달콤한 카라멜 시럽이 올라간 원조 시그니처 빙수").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(lucid).name("체리 빙수").price(17500).description("상큼한 생체리가 가득 올라간 시즌 한정 눈꽃빙수").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(lucid).name("커스터드 푸딩").price(6500).description("진한 바닐라빈 향이 매력적인 일본식 수제 푸딩").isRepresentative(false).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(lucidMenus);

        // 4) 슬로우터틀 동성로점
        Store slowTurtle = Store.builder()
                .themeStreet(dongseongro)
                .name("슬로우터틀 동성로점")
                .address("대구 중구 동성로2길 18-14")
                .latitude(new BigDecimal("35.8675000"))
                .longitude(new BigDecimal("128.5968000"))
                .businessHours("매일 11:00 - 22:30")
                .contact("053-256-5196")
                .coverImageUrl("https://images.unsplash.com/photo-1578985545062-69928b1d9587")
                .isPartner(true)
                .partnerDiscountNotice("디저팅 커플 세트 주문 시 아메리카노 1잔 리필 무료")
                .build();
        storeRepository.save(slowTurtle);

        List<StoreMenu> turtleMenus = List.of(
                StoreMenu.builder().store(slowTurtle).name("생딸기 와플").price(15800).description("주문 즉시 갓 구운 벨기에 와플에 동물성 생크림과 생딸기 토핑").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(slowTurtle).name("딸기 프레지에 케이크").price(8500).description("달콤한 마스카포네 크림과 신선한 딸기가 층층이 쌓인 케이크").isRepresentative(true).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(turtleMenus);

        // 5) 딥커피로스터스
        Store deepCoffee = Store.builder()
                .themeStreet(kimGwangseok)
                .name("딥커피로스터스 (Deep Coffee Roasters)")
                .address("대구 중구 봉산문화길 43")
                .latitude(new BigDecimal("35.8631000"))
                .longitude(new BigDecimal("128.5997000"))
                .businessHours("매일 10:00 - 20:00 (월요일 휴무)")
                .contact("053-255-0811")
                .coverImageUrl("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd")
                .isPartner(true)
                .partnerDiscountNotice("디저팅 회원 에스프레소 메뉴 주문 시 바치디다마 쿠키 1개 증정")
                .build();
        storeRepository.save(deepCoffee);

        List<StoreMenu> deepMenus = List.of(
                StoreMenu.builder().store(deepCoffee).name("바치디다마 (Baci di Dama)").price(3500).description("이탈리아 피에몬테 전통 수제 초콜릿 샌드 쿠키").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(deepCoffee).name("에스프레소 아라냐").price(3800).description("진한 에스프레소 위에 부드러운 수제 크림과 카카오 파우더").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(deepCoffee).name("바닐라 로마노").price(4200).description("상큼한 레몬 즙과 바닐라빈 시럽이 어우러진 특색 있는 에스프레소").isRepresentative(false).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(deepMenus);

        // =========================================================================
        // 7. 예약 슬롯 개설 (오늘, 내일, 모레 x 4개 시간대 x 5개 매장 = 총 60개 슬롯)
        // =========================================================================
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = List.of(today, today.plusDays(1), today.plusDays(2));
        List<LocalTime> slotTimes = List.of(LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(20, 0));
        List<Store> allStores = List.of(anook, capella, lucid, slowTurtle, deepCoffee);

        List<StoreReservationSlot> slots = new ArrayList<>();
        for (Store store : allStores) {
            for (LocalDate date : targetDates) {
                for (LocalTime time : slotTimes) {
                    slots.add(StoreReservationSlot.builder()
                            .store(store)
                            .slotDate(date)
                            .slotTime(time)
                            .maxCapacity(4)
                            .currentReservedCount(0)
                            .isAvailable(true)
                            .build());
                }
            }
        }
        slotRepository.saveAll(slots);

        // =========================================================================
        // 8. 7% 플랫폼 수수료 & 매장 정산 시연용 예약 라이프사이클 데이터
        // =========================================================================
        // 루시드(lucid): 총 완료 매출 120,000원 -> 7% 수수료 8,400원, 정산액 111,600원
        List<Reservation> lucidReservations = List.of(
                // 1) 방문 완료 건들 (COMPLETED)
                Reservation.builder()
                        .user(userMinjung).store(lucid)
                        .reservationDate(today.minusDays(3)).reservationTime(LocalTime.of(14, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(35500) // 푸딩빙수 + 체리빙수
                        .specialRequest("창가 커플석 부탁드립니다.").build(),
                Reservation.builder()
                        .user(userSoyeon).store(lucid)
                        .reservationDate(today.minusDays(2)).reservationTime(LocalTime.of(16, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(24500) // 푸딩빙수 + 커스터드푸딩
                        .specialRequest("기념일 방문입니다!").build(),
                Reservation.builder()
                        .user(userYuna).store(lucid)
                        .reservationDate(today.minusDays(1)).reservationTime(LocalTime.of(18, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(42000) // 빙수 2개 + 푸딩 1개
                        .specialRequest("조용한 자리로 부탁드려요.").build(),
                Reservation.builder()
                        .user(userTaehyun).store(lucid)
                        .reservationDate(today.minusDays(1)).reservationTime(LocalTime.of(20, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(18000) // 푸딩빙수 1개
                        .specialRequest(null).build(),

                // 2) 점주 접수 및 준비 중 건들 (CONFIRMED, PREPARING)
                Reservation.builder()
                        .user(userMinjung).store(lucid)
                        .reservationDate(today).reservationTime(LocalTime.of(18, 0))
                        .guestCount(2).status(ReservationStatus.PREPARING).totalAmount(35500)
                        .specialRequest("도착 10분 전입니다!").build(),
                Reservation.builder()
                        .user(userJihun).store(lucid)
                        .reservationDate(today.plusDays(1)).reservationTime(LocalTime.of(14, 0))
                        .guestCount(2).status(ReservationStatus.CONFIRMED).totalAmount(24500)
                        .specialRequest("데이트 첫 만남입니다 잘 부탁드립니다.").build(),

                // 3) 취소 건 (CANCELLED)
                Reservation.builder()
                        .user(userYuna).store(lucid)
                        .reservationDate(today.minusDays(4)).reservationTime(LocalTime.of(16, 0))
                        .guestCount(2).status(ReservationStatus.CANCELLED).totalAmount(18000)
                        .specialRequest("개인 사정으로 취소합니다.").build()
        );
        reservationRepository.saveAll(lucidReservations);

        // 아눅(anook): 총 완료 매출 50,000원 -> 7% 수수료 3,500원, 정산액 46,500원
        List<Reservation> anookReservations = List.of(
                Reservation.builder()
                        .user(userTaehyun).store(anook)
                        .reservationDate(today.minusDays(2)).reservationTime(LocalTime.of(15, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(28000) // 치즈케이크 + 크루키 2개 + 라떼
                        .specialRequest("앞산 전망 좋은 자리로 부탁드려요.").build(),
                Reservation.builder()
                        .user(userMinjung).store(anook)
                        .reservationDate(today.minusDays(1)).reservationTime(LocalTime.of(17, 0))
                        .guestCount(2).status(ReservationStatus.COMPLETED).totalAmount(22000)
                        .specialRequest("크루키 품절되지 않게 부탁드려요!").build()
        );
        reservationRepository.saveAll(anookReservations);

        log.info("🎉 [단디톤 2026] 전 도메인 실감형 종합 목업 데이터 시딩이 성공적으로 완료되었습니다!");
        log.info("   - 등록 유저: 관리자 1명, 점주 2명, 데이팅 유저 5명 (총 8명)");
        log.info("   - 대구 테마거리: 앞산 카페거리, 동성로 통신골목, 김광석길 (3곳)");
        log.info("   - 제휴 디저트 카페: 아눅앞산, 카펠라, 카페루시드, 슬로우터틀, 딥커피로스터스 (5곳)");
        log.info("   - 예약 슬롯: 5개 매장 x 3일 x 4타임 (총 60개 슬롯)");
        log.info("   - STOMP 채팅방: 2개 개설 (MongoDB PLACE_PROPOSAL 매장 제안 메시지 포함 8건 적재)");
        log.info("   - 7% 정산 시연 데이터: 카페루시드(4건 완료 120,000원 -> 수수료 8,400원), 아눅앞산(2건 50,000원)");
    }
}
