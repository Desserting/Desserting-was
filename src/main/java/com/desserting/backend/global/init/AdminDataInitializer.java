package com.desserting.backend.global.init;

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
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ThemeStreetRepository themeStreetRepository;
    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 && storeRepository.count() > 0) {
            log.info("초기 목업 데이터가 이미 존재합니다. 시딩을 건너뜁니다.");
            return;
        }

        log.info("대구 로컬 디저트 카페 및 테마거리 초기 목업 데이터 시딩을 시작합니다...");

        // 1. 관리자 및 점주 유저 생성
        User adminUser = User.builder()
                .email("admin@desserting.com")
                .password(passwordEncoder.encode("admin1234!"))
                .nickname("디저팅운영자")
                .gender("MALE")
                .age(30)
                .region("대구 중구")
                .role(UserRole.ROLE_ADMIN)
                .isPremium(true)
                .heartCount(999)
                .dessertTags(new ArrayList<>())
                .build();
        userRepository.save(adminUser);

        User storeOwner = User.builder()
                .email("owner@lucid.com")
                .password(passwordEncoder.encode("owner1234!"))
                .nickname("루시드점주")
                .gender("FEMALE")
                .age(35)
                .region("대구 중구")
                .role(UserRole.ROLE_STORE_OWNER)
                .isPremium(false)
                .heartCount(10)
                .dessertTags(new ArrayList<>())
                .build();
        userRepository.save(storeOwner);

        User normalUser = User.builder()
                .email("tester@desserting.com")
                .password(passwordEncoder.encode("test1234!"))
                .nickname("딸기휘낭시에")
                .gender("FEMALE")
                .age(26)
                .region("대구 수성구")
                .role(UserRole.ROLE_USER)
                .isPremium(true)
                .heartCount(5)
                .dessertTags(new ArrayList<>())
                .build();
        normalUser.getDessertTags().add(UserDessertTag.builder().user(normalUser).dessertType(DessertType.BAKERY).build());
        normalUser.getDessertTags().add(UserDessertTag.builder().user(normalUser).dessertType(DessertType.ICE_CREAM_BINGSU).build());
        userRepository.save(normalUser);

        // 2. 테마거리 등록 (앞산 카페거리 & 동성로 통신골목 카페거리)
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

        // 3. 제휴 매장 등록
        Store lucid = Store.builder()
                .themeStreet(dongseongro)
                .name("카페 루시드")
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

        Store anook = Store.builder()
                .themeStreet(apsan)
                .name("아눅 앞산")
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

        // 4. 대표 디저트 메뉴 등록
        List<StoreMenu> lucidMenus = List.of(
                StoreMenu.builder().store(lucid).name("푸딩 빙수").price(18000).description("부드러운 커스터드 푸딩과 달콤한 카라멜 시럽이 올라간 시그니처 빙수").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(lucid).name("체리 빙수").price(17500).description("상큼한 생체리가 가득 올라간 시즌 한정 눈꽃빙수").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(lucid).name("커스터드 푸딩").price(6500).description("진한 바닐라빈 향이 매력적인 일본식 수제 푸딩").isRepresentative(false).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(lucidMenus);

        List<StoreMenu> anookMenus = List.of(
                StoreMenu.builder().store(anook).name("바스크 치즈케이크").price(8000).description("스페인 전통 방식으로 고온에서 진하게 구워낸 글루텐프리 치즈케이크").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(anook).name("피스타치오 크루키").price(6800).description("바삭한 크루아상과 진한 피스타치오 쿠키 도우의 만남").isRepresentative(true).isSoldOut(false).build(),
                StoreMenu.builder().store(anook).name("아눅 크림 라떼").price(6500).description("달콤하고 묵직한 수제 생크림이 올라간 시그니처 커피").isRepresentative(false).isSoldOut(false).build()
        );
        storeMenuRepository.saveAll(anookMenus);

        // 5. 오늘 및 내일 데이트 예약 슬롯 개설
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        List<LocalTime> slotTimes = List.of(LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(20, 0));

        List<StoreReservationSlot> slots = new ArrayList<>();
        for (LocalDate date : List.of(today, tomorrow)) {
            for (LocalTime time : slotTimes) {
                slots.add(StoreReservationSlot.builder().store(lucid).slotDate(date).slotTime(time).maxCapacity(4).currentReservedCount(0).isAvailable(true).build());
                slots.add(StoreReservationSlot.builder().store(anook).slotDate(date).slotTime(time).maxCapacity(4).currentReservedCount(0).isAvailable(true).build());
            }
        }
        slotRepository.saveAll(slots);

        // 6. 7% 수수료 및 정산 시연을 위한 완료된 모의 예약 2건 생성
        Reservation completedRes1 = Reservation.builder()
                .user(normalUser)
                .store(lucid)
                .reservationDate(today.minusDays(1))
                .reservationTime(LocalTime.of(16, 0))
                .guestCount(2)
                .status(ReservationStatus.COMPLETED)
                .totalAmount(24500) // 푸딩빙수 + 커스터드푸딩 등
                .specialRequest("창가 자리 부탁드립니다.")
                .build();

        Reservation completedRes2 = Reservation.builder()
                .user(normalUser)
                .store(lucid)
                .reservationDate(today.minusDays(2))
                .reservationTime(LocalTime.of(18, 0))
                .guestCount(2)
                .status(ReservationStatus.COMPLETED)
                .totalAmount(35500) // 빙수 2개
                .specialRequest("생일 기념 방문입니다!")
                .build();

        reservationRepository.saveAll(List.of(completedRes1, completedRes2));

        log.info("대구 로컬 디저트 카페 초기 목업 데이터 시딩이 성공적으로 완료되었습니다!");
    }
}
