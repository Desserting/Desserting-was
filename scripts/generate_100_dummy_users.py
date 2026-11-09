import random

# Areas definition based on screenshots
# 10 Living Areas: 5 Daegu, 5 Gyeongbuk
AREAS = [
    # Daegu 5 Areas
    {
        "code": "daegu_mid",
        "name": "대구 중부",
        "regions": ["대구 중구", "대구 남구"],
        "center_lat": 35.8687,
        "center_lng": 128.5982,
        "female_names": ["삼덕동푸딩", "동성로요정", "교동바스크", "앞산크루키", "대명동휘낭"],
        "male_names": ["통신골목핸드드립", "앞산전망대", "삼덕에스프레소", "교동필터커피", "대봉동브루잉"],
        "bios": [
            "삼덕동 골목 푸딩 카페 도장깨기 중이에요 🍮",
            "동성로 조용한 에스프레소 바 추천해 드려요 ☕",
            "앞산 뷰 좋은 카페에서 디저트 먹으며 힐링해요",
            "대명동 빨래터 쪽 드라이브 겸 카페 가실 분!",
            "갓 구운 휘낭시에와 필터커피 한 잔의 여유를 좋아합니다"
        ]
    },
    {
        "code": "daegu_east",
        "name": "대구 동부",
        "regions": ["대구 동구", "대구 수성구"],
        "center_lat": 35.8580,
        "center_lng": 128.6300,
        "female_names": ["수성못말차", "범어동스콘", "동대구베이글", "황금동마카롱", "만촌동파이"],
        "male_names": ["수성레이크러버", "범어핸드드립", "신천동브런치", "동대구역에쏘", "시지딸기케익"],
        "bios": [
            "수성못 산책하고 조용한 카페 가는 데이트 선호해요",
            "범어동 스페셜티 커피 전문점 같이 갈 분 찾아요",
            "동대구역 신상 베이글 맛집 투어 메이트 구합니다 🥯",
            "달지 않고 진한 말차 테린느 좋아해요 🍵",
            "주말 오전에 여유롭게 브런치 즐기는 거 좋아해요"
        ]
    },
    {
        "code": "daegu_nw",
        "name": "대구 서북부",
        "regions": ["대구 북구", "대구 서구"],
        "center_lat": 35.8850,
        "center_lng": 128.5830,
        "female_names": ["경북대북문케익", "침산동와플", "칠곡푸딩이", "복현동크로플", "비산동약과"],
        "male_names": ["북문라떼장인", "침산동드립", "칠곡스페셜티", "노원동아아", "원대동티타임"],
        "bios": [
            "경북대 북문 가성비 좋고 맛있는 카페 탐방 좋아해요",
            "칠곡 3지구 디저트 맛집 도장깨기 중입니다 🍰",
            "침산동 분위기 좋은 루프탑 카페 같이 가요",
            "바삭한 벨기에 와플이랑 달콤한 아이스크림 조합 최고!",
            "식후에 조용한 카페에서 소소한 대화 나누고 싶어요"
        ]
    },
    {
        "code": "daegu_sw",
        "name": "대구 서남부",
        "regions": ["대구 달서구", "대구 달성군"],
        "center_lat": 35.8300,
        "center_lng": 128.5320,
        "female_names": ["상인동타르트", "월성동치즈케익", "진천동달콤이", "다사베이커리", "현풍도넛"],
        "male_names": ["월배에스프레소", "상인역드립", "유가읍크루아상", "송현동커피", "도원동산책"],
        "bios": [
            "월성동 수제 생과일 타르트 찐맛집 알아요 🥧",
            "상인동 카페골목에서 맛있는 커피 마셔요",
            "주말에 강정보나 송해공원 드라이브 겸 카페 어떠세요?",
            "진한 치즈케이크와 시원한 아메리카노 러버입니다",
            "현풍 테크노폴리스 감성 카페 투어 좋아합니다"
        ]
    },
    {
        "code": "daegu_gw",
        "name": "대구 군위",
        "regions": ["대구 군위군"],
        "center_lat": 36.2428,
        "center_lng": 128.5728,
        "female_names": ["화본역주악", "군위대추라떼", "팔공산자락", "사유원티룸", "삼국유사달콤"],
        "male_names": ["군위드라이브", "화본역커피", "팔공산베이커리", "우보면필터", "의흥면디저트"],
        "bios": [
            "화본역 레트로 감성과 전통 디저트 좋아해요 🍡",
            "사유원 근처 고즈넉한 찻집 투어가 취미입니다",
            "주말에 팔공산 터널 지나 군위 대형 카페 드라이브 가요!",
            "대추 디저트랑 전통 한과 맛보러 갈 분 환영합니다",
            "공기 좋은 곳에서 여유롭게 즐기는 커피가 최고예요"
        ]
    },
    # Gyeongbuk 5 Areas
    {
        "code": "gb_near",
        "name": "대구 인접권",
        "regions": ["경북 경산시", "경북 칠곡군", "경북 영천시"],
        "center_lat": 35.8250,
        "center_lng": 128.7400,
        "female_names": ["영남대오렌지", "중방동마카롱", "하양딸기빙수", "왜관철교라떼", "영천샤인타르트"],
        "male_names": ["경산핸드드립", "영대감성에쏘", "칠곡왜관브루잉", "하양역바리스타", "영천블렌드"],
        "bios": [
            "영남대 오렌지거리 맛있는 디저트 카페 다 알아요 🍊",
            "왜관 낙동강 뷰 보이는 대형 베이커리 카페 추천해요",
            "달콤한 샤인머스캣 타르트 함께 먹으러 가요 🍇",
            "하양 대가대 근처 조용한 스터디형 카페 좋아합니다",
            "퇴근 후 경산 남매지 산책하고 카페 가는 코스 좋아해요"
        ]
    },
    {
        "code": "gb_east",
        "name": "경북 동해안권",
        "regions": ["경북 포항시", "경북 경주시"],
        "center_lat": 35.8562,
        "center_lng": 129.2247,
        "female_names": ["황리단길푸딩", "영일대오션뷰", "보문호수티룸", "환호공원스콘", "구룡포과즐"],
        "male_names": ["황남동에스프레소", "포항오션드립", "보문단지커피", "영일대바다바람", "불국사단풍길"],
        "bios": [
            "경주 황리단길 한옥 카페에서 고즈넉하게 데이트해요 🏯",
            "포항 영일대 파도 보면서 시원한 커피 마시는 거 좋아해요 🌊",
            "보문호수 뷰 보면서 생과일 케이크 먹어요",
            "영일대 해안로 오션뷰 카페 메이트 찾습니다",
            "황리단길 십원빵이랑 젤라또 먹으러 갈 분 구해요!"
        ]
    },
    {
        "code": "gb_west",
        "name": "경북 서부권",
        "regions": ["경북 구미시", "경북 김천시", "경북 상주시"],
        "center_lat": 36.1195,
        "center_lng": 128.3446,
        "female_names": ["금리단길마카롱", "직지사전통차", "금오산수플레", "혁신도시케익", "상주곶감라떼"],
        "male_names": ["금오산올레길", "구미역드립", "김천혁신에쏘", "직지사길목", "상주자전거길"],
        "bios": [
            "구미 금리단길 아기자기한 감성 카페 좋아해요 ☕",
            "김천 직지사 근처 전통 찻집에서 여유 즐겨요",
            "금오산 산책하고 수플레 팬케이크 먹으러 가요 🥞",
            "김천혁신도시 신상 디저트 카페 투어 가실 분!",
            "상주 곶감 디저트랑 고소한 라떼 조합 추천해요"
        ]
    },
    {
        "code": "gb_north",
        "name": "경북 북부권",
        "regions": ["경북 안동시", "경북 영주시", "경북 예천군"],
        "center_lat": 36.5684,
        "center_lng": 128.7294,
        "female_names": ["월영교달빵", "하회마을식혜", "영주사과파이", "예천용궁순대길", "도산서원약과"],
        "male_names": ["안동호수드립", "영주부석사커피", "월영교야경에쏘", "예천회룡포길", "안동갈비골목차"],
        "bios": [
            "안동 월영교 야경 보며 달콤한 빵이랑 차 한잔해요 🌙",
            "영주 명품 사과로 만든 수제 사과파이 좋아합니다 🍎",
            "하회마을 한옥 마루에서 즐기는 전통 약과 데이트!",
            "예천 회룡포 드라이브하고 조용한 카페 가요",
            "스페셜티 커피와 전통 디저트의 페어링을 즐깁니다"
        ]
    },
    {
        "code": "gb_nw",
        "name": "경북 북서권",
        "regions": ["경북 문경시", "경북 상주시"],
        "center_lat": 36.5937,
        "center_lng": 128.1866,
        "female_names": ["문경오미자티", "새재사과타르트", "상주곶감주악", "가은역스콘", "점촌달콤이"],
        "male_names": ["문경새재필터", "상주낙동강드립", "문경온천에쏘", "새재길바리스타", "가은철로커피"],
        "bios": [
            "문경새재 단풍 보고 상큼한 오미자 에이드 마셔요 🍹",
            "문경 가은역 레트로 철도 카페 데이트 가실 분!",
            "상주 낙동강 뷰 보면서 즐기는 디저트 타임 좋아해요",
            "새재 사과로 구운 따뜻한 타르트 추천해 드립니다",
            "조용하고 풍경 좋은 자연 속 카페 힐링을 선호해요"
        ]
    }
]

DESSERT_TYPES = [
    ("CAKE", "cake.png"),
    ("BAKERY", "bakery.png"),
    ("COOKIE_FINANCIER", "cookie.png"),
    ("TART_PIE", "tart.png"),
    ("ICE_CREAM_BINGSU", "bingsu.png"),
    ("COFFEE", "coffee.png"),
    ("TEA_NON_COFFEE", "tea.png"),
    ("TRADITIONAL_KOREAN", "traditional.png")
]

# BCrypt hash for "test1234!"
PASSWORD_HASH = "$2a$10$wT2iJ2i2uYxVlB6c7P9QvOdW8d6mH1Q2uE0vO3uO4uO5uO6uO7uO8"

def generate_sql():
    user_inserts = []
    tag_inserts = []
    
    user_count = 0
    for area in AREAS:
        # 5 females, 5 males
        for i in range(5):
            # Female
            user_count += 1
            f_email = f"dummy_{area['code']}_f{i+1:02d}@desserting.com"
            f_nick = area["female_names"][i]
            f_bio = area["bios"][i]
            f_age = random.randint(22, 28)
            f_region = area["regions"][i % len(area["regions"])]
            f_lat = round(area["center_lat"] + random.uniform(-0.008, 0.008), 6)
            f_lng = round(area["center_lng"] + random.uniform(-0.008, 0.008), 6)
            # Pick primary avatar & 2 tags
            prim_type, prim_avatar = DESSERT_TYPES[(i * 2) % len(DESSERT_TYPES)]
            sec_type, _ = DESSERT_TYPES[(i * 2 + 1) % len(DESSERT_TYPES)]
            f_avatar_url = f"https://desserting-web.vercel.app/avatars/{prim_avatar}"

            user_inserts.append(
                f"('{f_email}', '{PASSWORD_HASH}', '{f_nick}', 'FEMALE', {f_age}, '{f_region}', "
                f"{f_lat}, {f_lng}, 10, false, true, '{f_avatar_url}', '{f_bio}', 'ROLE_USER', NOW(), NOW())"
            )
            tag_inserts.append(f"((SELECT id FROM users WHERE email='{f_email}'), '{prim_type}', NOW(), NOW())")
            tag_inserts.append(f"((SELECT id FROM users WHERE email='{f_email}'), '{sec_type}', NOW(), NOW())")

            # Male
            user_count += 1
            m_email = f"dummy_{area['code']}_m{i+1:02d}@desserting.com"
            m_nick = area["male_names"][i]
            m_bio = area["bios"][(i + 2) % len(area["bios"])]
            m_age = random.randint(25, 32)
            m_region = area["regions"][(i + 1) % len(area["regions"])]
            m_lat = round(area["center_lat"] + random.uniform(-0.008, 0.008), 6)
            m_lng = round(area["center_lng"] + random.uniform(-0.008, 0.008), 6)
            m_prim_type, m_prim_avatar = DESSERT_TYPES[(i * 2 + 3) % len(DESSERT_TYPES)]
            m_sec_type, _ = DESSERT_TYPES[(i * 2 + 4) % len(DESSERT_TYPES)]
            m_avatar_url = f"https://desserting-web.vercel.app/avatars/{m_prim_avatar}"

            user_inserts.append(
                f"('{m_email}', '{PASSWORD_HASH}', '{m_nick}', 'MALE', {m_age}, '{m_region}', "
                f"{m_lat}, {m_lng}, 10, false, true, '{m_avatar_url}', '{m_bio}', 'ROLE_USER', NOW(), NOW())"
            )
            tag_inserts.append(f"((SELECT id FROM users WHERE email='{m_email}'), '{m_prim_type}', NOW(), NOW())")
            tag_inserts.append(f"((SELECT id FROM users WHERE email='{m_email}'), '{m_sec_type}', NOW(), NOW())")

    sql = [
        "BEGIN;",
        "-- Ensure is_dummy column exists",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_dummy BOOLEAN NOT NULL DEFAULT false;",
        "",
        "-- Delete existing dummy users if any to avoid collision",
        "DELETE FROM users WHERE email LIKE 'dummy_%';",
        "",
        "-- Insert 100 dummy users",
        "INSERT INTO users (email, password, nickname, gender, age, region, latitude, longitude, heart_count, is_premium, is_dummy, dessert_avatar_url, bio, role, created_at, updated_at)",
        "VALUES\n" + ",\n".join(user_inserts) + ";",
        "",
        "-- Insert 200 dessert tags for dummy users",
        "INSERT INTO user_dessert_tags (user_id, dessert_type, created_at, updated_at)",
        "VALUES\n" + ",\n".join(tag_inserts) + ";",
        "",
        "COMMIT;"
    ]
    
    with open("/tmp/inject_100_dummy_users.sql", "w", encoding="utf-8") as f:
        f.write("\n".join(sql))
    print(f"Generated SQL for {user_count} users successfully at /tmp/inject_100_dummy_users.sql")

if __name__ == "__main__":
    generate_sql()
