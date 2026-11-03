package com.desserting.backend.global.init;

import com.desserting.backend.domain.reservation.entity.ReservationSlot;
import com.desserting.backend.domain.reservation.repository.ReservationSlotRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.theme.dto.Coordinate;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import com.desserting.backend.domain.theme.service.ThemeBoundaryParser;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬(local 프로필) 시연용 데이터.
 * - 테마거리 3곳 + 제휴매장·메뉴 (DB에 테마거리가 하나도 없을 때만)
 * - 모든 제휴매장의 앞으로 14일 예약 슬롯 (없는 날짜만 채움, 매장 관리자 SELL-04 구현 전 임시)
 * - 랜덤 매칭 테스트용 유저 demo1~6@desserting.com / password123! (없을 때만)
 * 끄려면 desserting.seed.enabled=false
 */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(name = "desserting.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalDemoDataInitializer implements ApplicationRunner {

    private static final int SLOT_DAYS = 14;
    private static final int SLOT_CAPACITY = 10;
    private static final LocalTime FIRST_SLOT = LocalTime.of(11, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(20, 0);
    private static final String DEMO_PASSWORD = "password123!";

    private final ThemeStreetRepository themeStreetRepository;
    private final StoreRepository storeRepository;
    private final ReservationSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ThemeBoundaryParser boundaryParser;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (themeStreetRepository.count() == 0) {
            seedThemeStreetsAndStores();
        }
        int slots = fillReservationSlots();
        int users = seedDemoUsers();
        log.info("[LocalDemoData] 테마거리 {}곳, 제휴매장 {}곳, 신규 슬롯 {}개, 신규 데모유저 {}명",
                themeStreetRepository.count(), storeRepository.count(), slots, users);
    }

    private void seedThemeStreetsAndStores() {
        ThemeStreet apsan = themeStreetRepository.save(theme("앞산 카페거리",
                "앞산 자락을 따라 카페와 베이커리가 모여 있는 대구 대표 카페거리",
                35.8290, 128.5852,
                List.of(new Coordinate(35.8322, 128.5800), new Coordinate(35.8322, 128.5905),
                        new Coordinate(35.8260, 128.5905), new Coordinate(35.8256, 128.5800))));
        ThemeStreet suseong = themeStreetRepository.save(theme("수성못 카페거리",
                "수성못 산책로를 따라 이어지는 호수뷰 디저트 카페 구역",
                35.8281, 128.6172,
                List.of(new Coordinate(35.8312, 128.6120), new Coordinate(35.8312, 128.6225),
                        new Coordinate(35.8250, 128.6225), new Coordinate(35.8250, 128.6120))));
        ThemeStreet dongseong = themeStreetRepository.save(theme("동성로 디저트거리",
                "대구 도심 동성로 골목의 베이커리·전통 디저트 밀집 구역",
                35.8690, 128.5955,
                List.of(new Coordinate(35.8720, 128.5920), new Coordinate(35.8720, 128.5990),
                        new Coordinate(35.8660, 128.5990), new Coordinate(35.8660, 128.5920))));

        List<Store> stores = new ArrayList<>();
        stores.add(store(apsan, "카페 올리브", "대구 남구 앞산순환로 451", 35.8297, 128.5871, "디저팅 회원 전 메뉴 10% 할인",
                menu("딸기 생크림 케이크", 7500, DessertType.CAKE, true),
                menu("바스크 치즈케이크", 7000, DessertType.CAKE, false),
                menu("아메리카노", 4500, DessertType.COFFEE, false)));
        stores.add(store(apsan, "베이크룸", "대구 남구 앞산순환로 475", 35.8285, 128.5838, "커플 방문 시 휘낭시에 1개 증정",
                menu("소금빵", 3500, DessertType.BAKERY, true),
                menu("휘낭시에 세트", 9000, DessertType.COOKIE_FINANCIER, false),
                menu("밀크티", 5500, DessertType.TEA_NON_COFFEE, false)));
        stores.add(store(apsan, "디저트하우스", "대구 남구 대명로 210", 35.8272, 128.5880, null,
                menu("과일 타르트", 6800, DessertType.TART_PIE, true),
                menu("핸드드립 커피", 6000, DessertType.COFFEE, false)));
        stores.add(store(apsan, "앞산 빙수당", "대구 남구 앞산순환로 503", 35.8306, 128.5822, "2인 빙수 주문 시 음료 1잔 무료",
                menu("우유 팥빙수", 11000, DessertType.ICE_CREAM_BINGSU, true),
                menu("인절미 빙수", 12000, DessertType.ICE_CREAM_BINGSU, false)));
        stores.add(store(suseong, "레이크 타르트", "대구 수성구 무학로 92", 35.8278, 128.6160, "디저팅 회원 음료 사이즈업",
                menu("에그타르트", 3200, DessertType.TART_PIE, true),
                menu("레몬 파이", 6500, DessertType.TART_PIE, false),
                menu("플랫화이트", 5500, DessertType.COFFEE, false)));
        stores.add(store(suseong, "수성 로스터리", "대구 수성구 용학로 35", 35.8290, 128.6195, null,
                menu("스페셜티 핸드드립", 7000, DessertType.COFFEE, true),
                menu("티라미수", 7500, DessertType.CAKE, false)));
        stores.add(store(dongseong, "크로와상 공방", "대구 중구 동성로2길 41", 35.8695, 128.5948, "오후 3시 이후 크로와상 1+1",
                menu("버터 크로와상", 4200, DessertType.BAKERY, true),
                menu("아이스크림 크로플", 8500, DessertType.ICE_CREAM_BINGSU, false)));
        stores.add(store(dongseong, "개성주악 연구소", "대구 중구 동성로6길 18", 35.8678, 128.5970, null,
                menu("개성주악 3구", 6000, DessertType.TRADITIONAL_KOREAN, true),
                menu("쌍화차", 6500, DessertType.TEA_NON_COFFEE, false)));
        storeRepository.saveAll(stores);
    }

    private int fillReservationSlots() {
        LocalDate today = LocalDate.now(clock);
        List<ReservationSlot> newSlots = new ArrayList<>();
        for (Store store : storeRepository.findAllByIsPartnerTrue()) {
            for (int d = 0; d < SLOT_DAYS; d++) {
                LocalDate date = today.plusDays(d);
                if (slotRepository.existsByStoreIdAndSlotDate(store.getId(), date)) {
                    continue;
                }
                for (LocalTime t = FIRST_SLOT; !t.isAfter(LAST_SLOT); t = t.plusHours(1)) {
                    newSlots.add(ReservationSlot.builder()
                            .store(store).slotDate(date).slotTime(t).capacity(SLOT_CAPACITY).build());
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
                new Demo("딸기케이크러버", "FEMALE", 24, "대구 남구", 35.8301, 128.5866,
                        "딸기 들어간 디저트면 다 좋아요", List.of(DessertType.CAKE, DessertType.COFFEE)),
                new Demo("크루아상러버", "MALE", 26, "대구 남구", 35.8279, 128.5890,
                        "갓 구운 빵 냄새를 좋아해요", List.of(DessertType.BAKERY, DessertType.CAKE)),
                new Demo("마카롱좋아", "FEMALE", 25, "대구 수성구", 35.8285, 128.6180,
                        "수성못 산책 후 디저트 한 입", List.of(DessertType.COOKIE_FINANCIER, DessertType.TEA_NON_COFFEE)),
                new Demo("쿠키메니아", "MALE", 27, "대구 중구", 35.8688, 128.5960,
                        "동성로 디저트 투어 같이 해요", List.of(DessertType.COOKIE_FINANCIER, DessertType.BAKERY)),
                new Demo("빙수요정", "FEMALE", 23, "경북 경산시", 35.8251, 128.7415,
                        "겨울에도 빙수는 진리", List.of(DessertType.ICE_CREAM_BINGSU)),
                new Demo("주악선비", "MALE", 29, "대구 남구", 35.8265, 128.5845,
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

    private ThemeStreet theme(String name, String description, double centerLat, double centerLng,
                              List<Coordinate> boundary) {
        return ThemeStreet.builder()
                .name(name)
                .description(description)
                .centerLatitude(BigDecimal.valueOf(centerLat))
                .centerLongitude(BigDecimal.valueOf(centerLng))
                .boundaryPolygonJson(boundaryParser.toGeoJson(boundary))
                .build();
    }

    private record MenuSeed(String name, int price, DessertType type, boolean representative) {
    }

    private static MenuSeed menu(String name, int price, DessertType type, boolean representative) {
        return new MenuSeed(name, price, type, representative);
    }

    private static Store store(ThemeStreet theme, String name, String address, double lat, double lng,
                               String discountNotice, MenuSeed... menus) {
        Store store = Store.builder()
                .themeStreet(theme)
                .name(name)
                .address(address)
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lng))
                .businessHours("매일 11:00 - 21:00")
                .contact("053-000-0000")
                .partnerDiscountNotice(discountNotice)
                .build();
        for (MenuSeed m : menus) {
            store.getMenus().add(StoreMenu.builder()
                    .store(store)
                    .name(m.name())
                    .price(m.price())
                    .dessertType(m.type())
                    .isRepresentative(m.representative())
                    .build());
        }
        return store;
    }
}
