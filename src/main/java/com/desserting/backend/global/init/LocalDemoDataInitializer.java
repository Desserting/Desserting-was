package com.desserting.backend.global.init;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreDessertTag;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreDessertTagRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreReservationSlotRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4-2·4-5·4-6 로컬 시연 보조 데이터.
 * 테마거리·매장·메뉴는 AdminDataInitializer(공용 목업)를 그대로 사용하고, 여기서는 그 위에 아래만 추가한다.
 * - 목업 매장의 디저트 태그 (store_dessert_tags, 지도 디저트 필터용)
 * - 제휴매장 예약 슬롯을 앞으로 14일까지 보충 (없는 시간대만 생성, 목업과 같은 14·16·18·20시 / 4팀)
 * - 위치정보가 있는 매칭 테스트 유저 demo1~6@desserting.com / password123!
 * 공용 목업 시딩이 끝난 뒤 실행되도록 ApplicationReadyEvent 에서 동작한다. 끄려면 desserting.seed.explore=false
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@ConditionalOnProperty(name = "desserting.seed.explore", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalDemoDataInitializer {

    private static final int SLOT_DAYS = 14;
    private static final int SLOT_CAPACITY = 4;
    private static final List<LocalTime> SLOT_TIMES =
            List.of(LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(20, 0));
    private static final String DEMO_PASSWORD = "password123!";

    /** 공용 목업 매장명 → 판매 디저트 */
    private static final Map<String, List<DessertType>> STORE_DESSERT_TAGS = Map.of(
            "아눅 앞산 (a.nook Apsan)", List.of(DessertType.CAKE, DessertType.BAKERY, DessertType.COFFEE),
            "카펠라 (Capella)", List.of(DessertType.CAKE),
            "카페 루시드 (Cafe Lucid)", List.of(DessertType.ICE_CREAM_BINGSU),
            "슬로우터틀 동성로점", List.of(DessertType.BAKERY, DessertType.CAKE),
            "딥커피로스터스 (Deep Coffee Roasters)", List.of(DessertType.COOKIE_FINANCIER, DessertType.COFFEE));

    private final StoreRepository storeRepository;
    private final StoreDessertTagRepository dessertTagRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        List<Store> partnerStores = storeRepository.findAllByIsPartnerTrue();
        int tags = seedDessertTags(partnerStores);
        int slots = fillReservationSlots(partnerStores);
        int users = seedDemoUsers();
        log.info("[ExploreDemoData] 디저트 태그 {}개, 예약 슬롯 {}개, 데모 유저 {}명 추가", tags, slots, users);
    }

    private int seedDessertTags(List<Store> stores) {
        List<StoreDessertTag> newTags = new ArrayList<>();
        for (Store store : stores) {
            List<DessertType> types = STORE_DESSERT_TAGS.get(store.getName());
            if (types == null || dessertTagRepository.existsByStoreId(store.getId())) {
                continue;
            }
            types.forEach(type -> newTags.add(StoreDessertTag.of(store, type)));
        }
        dessertTagRepository.saveAll(newTags);
        return newTags.size();
    }

    private int fillReservationSlots(List<Store> stores) {
        LocalDate today = LocalDate.now(clock);
        List<StoreReservationSlot> newSlots = new ArrayList<>();
        for (Store store : stores) {
            for (int d = 0; d < SLOT_DAYS; d++) {
                LocalDate date = today.plusDays(d);
                for (LocalTime time : SLOT_TIMES) {
                    if (slotRepository.existsByStoreIdAndSlotDateAndSlotTime(store.getId(), date, time)) {
                        continue;
                    }
                    newSlots.add(StoreReservationSlot.builder()
                            .store(store).slotDate(date).slotTime(time).maxCapacity(SLOT_CAPACITY).build());
                }
            }
        }
        slotRepository.saveAll(newSlots);
        return newSlots.size();
    }

    private int seedDemoUsers() {
        record Demo(String nickname, String gender, int age, String region, double lat, double lng, String bio,
                    List<DessertType> tags) {
        }
        List<Demo> demos = List.of(
                new Demo("딸기케이크러버", "FEMALE", 24, "대구 남구", 35.8330, 128.5840,
                        "딸기 들어간 디저트면 다 좋아요", List.of(DessertType.CAKE, DessertType.COFFEE)),
                new Demo("크루아상러버", "MALE", 26, "대구 남구", 35.8312, 128.5862,
                        "갓 구운 빵 냄새를 좋아해요", List.of(DessertType.BAKERY, DessertType.CAKE)),
                new Demo("마카롱좋아", "FEMALE", 25, "대구 수성구", 35.8285, 128.6180,
                        "수성못 산책 후 디저트 한 입", List.of(DessertType.COOKIE_FINANCIER, DessertType.TEA_NON_COFFEE)),
                new Demo("쿠키메니아", "MALE", 27, "대구 중구", 35.8688, 128.5990,
                        "동성로 디저트 투어 같이 해요", List.of(DessertType.COOKIE_FINANCIER, DessertType.BAKERY)),
                new Demo("빙수요정", "FEMALE", 23, "경북 경산시", 35.8251, 128.7415,
                        "겨울에도 빙수는 진리", List.of(DessertType.ICE_CREAM_BINGSU)),
                new Demo("주악선비", "MALE", 29, "대구 남구", 35.8300, 128.5825,
                        "전통 디저트와 따뜻한 차를 좋아합니다", List.of(DessertType.TRADITIONAL_KOREAN, DessertType.TEA_NON_COFFEE)));

        int created = 0;
        for (int i = 0; i < demos.size(); i++) {
            Demo d = demos.get(i);
            String email = "demo" + (i + 1) + "@desserting.com";
            if (userRepository.existsByEmail(email) || userRepository.existsByNickname(d.nickname())) {
                continue;
            }
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(DEMO_PASSWORD))
                    .nickname(d.nickname())
                    .gender(d.gender())
                    .age(d.age())
                    .region(d.region())
                    .latitude(BigDecimal.valueOf(d.lat()))
                    .longitude(BigDecimal.valueOf(d.lng()))
                    .bio(d.bio())
                    .dessertAvatarUrl(d.tags().get(0).getDefaultAvatarUrl())
                    .build();
            d.tags().forEach(tag -> user.getDessertTags().add(UserDessertTag.of(user, tag)));
            userRepository.save(user);
            created++;
        }
        return created;
    }
}
