# 🍰 디저팅 프론트엔드 API 연동 가이드

> 화면(와이어프레임 ID)별로 **어떤 API를 어떤 순서로 부르는지**, Swagger에 없는 **실시간 채팅(STOMP)**, **에러 코드 처리**를 정리한 문서입니다.
> 요청·응답 필드의 자세한 형식은 Swagger를 기준으로 합니다.
>
> - Swagger UI: `https://desserting-api.duckdns.org/swagger-ui/index.html` (로컬은 `http://localhost:8080/swagger-ui/index.html`)
> - OpenAPI JSON: `{BASE_URL}/v3/api-docs`
> - ⚠️ `{BASE_URL}`은 **백엔드 서버 주소**(GCP 시연 `https://desserting-api.duckdns.org` / 로컬 `http://localhost:8080`)입니다. Vite 개발 서버(`localhost:5173`)는 `/api`, `/ws`만 프록시하므로 `5173/v3/api-docs`로는 열리지 않습니다.
> - 기준 코드: `main` — 2026-11-05

---

## 1. 기본 규칙

### 1.1 서버 및 프론트엔드 배포 주소
| 구분 | 접속 주소 | 비고 |
|---|---|---|
| 프론트엔드 (배포) | `https://desserting-web.vercel.app/` | Vercel 공식 배포 웹앱 |
| 백엔드 시연(GCP Cloud) | `https://desserting-api.duckdns.org` | 24/7 서울 리전 클라우드 호스팅, 정식 SSL(HTTPS) 적용 완료 |
| 백엔드 로컬 | `http://localhost:8080` | README 방법 A/B로 실행 |

- 모든 REST API는 `/api/v1`로 시작합니다.
- 실시간 채팅 웹소켓은 `wss://desserting-api.duckdns.org/ws` (로컬 `ws://localhost:8080/ws`) 입니다 (4장).

### 1.2 인증
- 로그인·회원가입 응답의 `accessToken`을 모든 요청에 `Authorization: Bearer {accessToken}`으로 보냅니다.
- 인증 없이 호출 가능한 API: `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`, `GET /health`, `GET /users`(프로필 목록), `GET /users/avatars`(기본 아바타 목록)

| 토큰 | 유효기간 | 용도 |
|---|---|---|
| accessToken | 1시간 | 모든 API, 웹소켓 CONNECT |
| refreshToken | 14일 | `POST /auth/refresh` 에만 사용 (다른 API에 넣으면 401) |

### 1.3 응답 형식
```jsonc
// 성공
{ "success": true, "message": "...", "data": { ... }, "timestamp": "2026-11-03T21:00:00" }

// 실패
{ "success": false, "code": "R001", "message": "선택하신 시간대의 예약 정원이 마감되었습니다.",
  "errors": [ { "field": "guestCount", "value": "0", "reason": "인원은 1명 이상이어야 합니다." } ],  // 입력값 검증 실패(C001)일 때만 채워짐
  "timestamp": "..." }
```
- 화면 분기는 HTTP 상태가 아니라 **`code`** 로 하는 것을 권장합니다 (6장).
- `message`는 사용자에게 그대로 보여줘도 되는 한국어 문구입니다.

### 1.4 날짜·시간 형식 (모두 한국 시간, 시간대 표기 없음)
| 종류 | 형식 | 예 |
|---|---|---|
| 날짜 | `YYYY-MM-DD` | `2026-11-15` |
| 시간 | 응답 `HH:mm:ss` / 요청 `HH:mm` 또는 `HH:mm:ss` | `15:00:00` |
| 일시 | `YYYY-MM-DDTHH:mm:ss` + 소수점 이하 초(0~9자리, 가변) | `2026-11-03T21:00:00.123456789` |

### 1.5 공통 코드값
| 이름 | 값 |
|---|---|
| 디저트 종류 `DessertType` | `CAKE` 케이크, `BAKERY` 베이커리, `COOKIE_FINANCIER` 구움과자, `TART_PIE` 타르트·파이, `ICE_CREAM_BINGSU` 빙수·아이스크림, `COFFEE` 커피, `TEA_NON_COFFEE` 차·논커피, `TRADITIONAL_KOREAN` 전통 디저트 |
| 성별 | `MALE`, `FEMALE` |
| 매칭 타입 | `SAME_TASTE` 같은 취향, `DIFFERENT_TASTE` 다른 취향 |
| 대화 요청 상태 | `PENDING` 대기, `ACCEPTED` 수락, `REJECTED` 거절, `EXPIRED` 24시간 만료 |
| 예약 상태 | `PENDING` 예약 대기, `CONFIRMED` 확정, `PREPARING` 준비 중, `COMPLETED` 완료, `CANCELLED` 취소 |
| 하트 내역 타입 | `WELCOME_BONUS` 가입 지급, `DAILY_CHARGE` 매일 충전, `REQUEST_DEDUCT` 요청 차감, `REQUEST_REFUND` 환불, `PURCHASE` 구매, `ADMIN_GRANT` 관리자 지급 |

---

## 2. 로그인 유지 (토큰 재발급)

⚠️ **토큰이 만료돼도 응답 코드는 `A003`이 아니라 `A001`(401)** 입니다. 만료와 미로그인을 구분할 수 없으므로 아래처럼 처리합니다.

1. 인증이 필요한 API에서 **401**을 받으면 `POST /api/v1/auth/refresh` `{ "refreshToken": "..." }` 를 **한 번만** 호출
2. 성공 → 새 `accessToken`·`refreshToken` 저장 후 원래 요청 재시도
3. 실패(401) → 토큰 삭제 후 로그인 화면으로 이동

```ts
// axios 예시
api.interceptors.response.use(undefined, async (error) => {
  const original = error.config;
  if (error.response?.status === 401 && !original._retried && !original.url.includes('/auth/')) {
    original._retried = true;
    try {
      const { data } = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken: store.refreshToken });
      store.setTokens(data.data.accessToken, data.data.refreshToken);
      original.headers.Authorization = `Bearer ${data.data.accessToken}`;
      return api(original);
    } catch {
      store.logout();            // 로그인 화면으로
    }
  }
  return Promise.reject(error);
});
```

---

## 3. 화면별 API 호출 순서

> 화면 ID는 `와이어프레임.png` 기준입니다. ❌ 표시는 백엔드 미구현입니다.

### ① 회원가입·프로필 (A-01 ~ A-04)
| 화면 | 동작 | API |
|---|---|---|
| A-01 시작 | 이메일 로그인 | `POST /auth/login` `{email, password}` → `data.accessToken` |
| A-01 회원가입 | 가입 + 자동 로그인 | `POST /auth/signup` (아래 필드) → 토큰 바로 발급 |
| A-03 취향 선택 | 가입 요청의 `dessertTypes`에 포함 | (별도 API 없음) |
| A-04 프로필 생성 | 닉네임·소개 수정 | `PATCH /users/me/profile` `{nickname?, bio?, region?, dessertAvatarUrl?, dessertTypes?}` |

- 회원가입 필드: `email`, `password`(6~50자), `nickname`(2~20자), `gender`, `age`(19~99), `region`(예: `대구 남구`), `dessertTypes`(1개 이상), `dessertAvatarUrl`(선택), `agreeToTerms`(true 필수)
- 선택 가능한 아바타 목록: `GET /users/avatars` (로그인 불필요) → `[{dessertType, description, avatarUrl}]`. `dessertAvatarUrl`(가입·프로필 수정)에는 이 목록의 `avatarUrl`만 허용되며, 그 외 값은 `400 U004`
- 대표 아바타는 첫 번째 디저트 취향으로 **자동 생성**됩니다 (`dessertAvatarUrl`).
  - 이미지 호스트: 프론트 Vercel `public/avatars/` → `https://desserting-web.vercel.app/avatars/{파일명}`
  - 매핑: `CAKE`→`cake.png`, `BAKERY`→`bakery.png`, `COOKIE_FINANCIER`→`cookie.png`, `TART_PIE`→`tart.png`, `ICE_CREAM_BINGSU`→`bingsu.png`, `COFFEE`→`coffee.png`, `TEA_NON_COFFEE`→`tea.png`, `TRADITIONAL_KOREAN`→`traditional.png`
- 가입 시 하트 5개 지급 (`WELCOME_BONUS`).
- ❌ 카카오·구글·애플 로그인은 미구현 (이메일 로그인만 가능)

### ② 홈 (B-01)
| 동작 | API |
|---|---|
| 내 정보·보유 하트·프리미엄 여부 | `GET /users/me` → `heartCount`, `isPremium`, `dessertTags` |

### ③ 랜덤 매칭·주변 사람 필터 (B-02, B-03)
**순서가 중요합니다.** 거리 필터를 쓰려면 매칭 전에 위치를 먼저 저장해야 합니다.

1. 위치 권한 **허용** 시: `PATCH /users/me/location` `{latitude, longitude, region?}` (위도 33~39, 경도 124~132)
2. 매칭 시작: `GET /matches/random?matchType=SAME_TASTE&radiusKm=10&region=대구`
3. 응답 처리
   - `data.found === true` → `data.candidate` 카드 표시
   - `data.found === false` → `data.guide` 문구 표시 (LOC-04 후보 없음 안내)
   - `data.guide`가 있으면(위치 없음 → 지역으로 대체 등) 토스트로 안내
4. 카드 버튼 및 상태 플래그
   - ✕ 다음 상대: **같은 API를 다시 호출** (오늘 본 상대는 자동 제외, 다 보면 `recycled: true`로 처음부터)
   - ♥ 좋아요: `POST /users/{userId}/like` (토글). 카드 내 `candidate.liked`가 `true`면 하트 활성 상태
   - → 대화 요청: `candidate.requested`가 `true`면 **"요청을 보냈어요"** 비활성 버튼 표시. `candidate.canRequest`가 `false`면 차단/하트부족/일반회원 미노출 상태이므로 버튼 비활성화

| 파라미터 | 설명 |
|---|---|
| `matchType` | 필수. `SAME_TASTE` / `DIFFERENT_TASTE` |
| `radiusKm` | 선택. 1~100. 내 좌표가 없으면 활동 지역 기준으로 자동 대체 (`appliedFilter: REGION`) |
| `region` | 선택. 지역명 앞부분 (`대구`, `대구 남구`). 쉼표로 여러 개 = OR (`대구 중구,대구 남구`). `전체`·`대구·경북`·미입력 = 전체 |
| `radiusKm` + `region` | 둘 다 만족(AND), `appliedFilter: DISTANCE_AND_REGION`. 내 좌표가 없으면 `region`만 적용(`REGION`) |

- 거리(`approxDistanceKm`)는 개인정보 보호를 위해 **정수 km 올림**으로만 옵니다 (예: `1` = 1km 이내).
- 차단 관계인 사용자, 이성이 아닌 사용자는 나오지 않습니다.

**후보 객체(`candidate`) 응답 예시**
```jsonc
{
  "userId": 2,
  "nickname": "딸기쇼트",
  "age": 24,
  "region": "대구 중구 삼덕동",
  "bio": "디저트와 커피를 사랑해요",
  "dessertAvatarUrl": "https://desserting-web.vercel.app/avatars/cake.png",
  "dessertTags": ["CAKE"],
  "commonDessertTags": ["CAKE"],
  "approxDistanceKm": 2,
  "requested": false,  // 내가 이미 대기(PENDING) 중인 대화 요청을 보냈는지 여부 (새로고침 시에도 유지)
  "liked": true,        // 내가 이미 이 유저에게 좋아요를 눌렀는지 여부 (새로고침 시에도 하트 유지)
  "canRequest": true   // 대화 요청 버튼 활성화 가능 여부 (false면 버튼 비활성화)
}
```

### ④ 프로필 탐색 (B-07)
| 동작 | API |
|---|---|
| 프로필 목록 | `GET /users` |
| 프로필 상세 | `GET /users/{userId}` → `OtherUserProfileResponse` (내 좋아요 여부 `liked` 포함) |
| 내가 좋아요한 사람 목록 | `GET /users/me/likes/sent` (내가 좋아요 누른 유저 목록 조회) |
| 좋아요 토글 | `POST /users/{userId}/like` |
| 신고 | `POST /users/{userId}/report` `{reason, description?}` |
| 차단 | `POST /users/{userId}/block?reason=...` |

### ⑤ 하트·대화 요청·수락함 (C-01)
| 화면 | 동작 | API |
|---|---|---|
| 대화 요청 | 요청 보내기 (하트 1개 차감, 프리미엄은 무료). 일반 회원은 **최근 24시간 랜덤 매칭으로 만난 상대**에게만, 프리미엄은 누구에게나 | `POST /matches/requests` `{receiverId}` |
| 하트 부족 | 하트가 부족한 경우 `400 H001` 반환 → 하트 구매 팝업 (⑨) 안내 | — |
| 사전 버튼 제어 | 후보 응답의 `canRequest` 플래그로 403 전에 버튼 비활성화 | `canRequest: false` (차단 / 이미요청 / 채팅방개설 / 하트부족 / 일반회원미노출) |
| 보낸 요청함 | 내가 보낸 요청 목록 (대기 중만 조회) | `GET /matches/requests/sent` → `data.requests[]` |
| 수락함 | 받은 요청 목록 (대기 중만) | `GET /matches/requests/received` |
| 수락함 | 최근 7일 처리된 요청까지 (새로고침 후 "수락 완료·채팅방 가기") | `GET /matches/requests/received?includeProcessed=true` → 수락 건은 `chatRoomId` 포함 |
| 수락함 | 수락 → 채팅방 생성 | `POST /matches/requests/{requestId}/accept` → `data.chatRoomId` 로 채팅방 이동 |
| 수락함 | 거절 (보낸 사람 하트 환불) | `POST /matches/requests/{requestId}/reject` |

- 24시간 동안 응답이 없으면 자동 만료되고 하트가 환불됩니다.
- 같은 상대에게 대기 중인 요청이 있거나 이미 채팅방이 있으면 `409 H002`.
- 일반 회원이 24시간 내 매칭된 적 없는 상대에게 프로필 화면에서 직접 요청을 시도하면 `403 A002` (프리미엄 안내 화면으로 유도).
- 이미 수락·거절된 요청을 다시 처리하면 `409 H005`, 만료된 요청이면 `410 H006`.

**보낸 요청 목록 (`GET /matches/requests/sent`) 응답 예시**
```jsonc
{
  "success": true,
  "data": {
    "requests": [
      {
        "requestId": 12,
        "receiverId": 5,
        "receiverNickname": "드립마니아",
        "receiverAvatarUrl": "https://desserting-web.vercel.app/avatars/coffee.png",
        "receiverAge": 28,
        "receiverRegion": "대구 남구",
        "receiverDessertTags": ["COFFEE"],
        "status": "PENDING",
        "createdAt": "2026-11-05T14:10:00",
        "expiredAt": "2026-11-06T14:10:00"
      }
    ]
  }
}
```

**받은 요청 목록 (`GET /matches/requests/received`) 응답 예시**
```jsonc
{ "requestId": 5,
  "senderId": 4, "senderNickname": "초코크로와상", "senderAvatarUrl": "https://desserting-web.vercel.app/avatars/bakery.png",
  "senderAge": 26, "senderRegion": "대구 수성구", "senderDessertTags": ["BAKERY"],
  "status": "PENDING",          // PENDING | ACCEPTED | REJECTED | EXPIRED
  "createdAt": "2026-11-05T08:47:22", "expiredAt": "2026-11-06T08:47:22",
  "chatRoomId": null }
```

### ⑥ 채팅 (C-02, C-03, C-04)
| 화면 | 동작 | API |
|---|---|---|
| C-02 채팅 목록 | 내 채팅방 | `GET /chats/rooms` → `roomId`, `otherUserNickname`, `lastMessagePreview`, `unreadCount` |
| C-03 채팅방 진입 | 이전 메시지 | `GET /chats/rooms/{roomId}/messages?size=30` (최신순) |
| C-03 위로 스크롤 | 더 이전 메시지 | `...?cursorId={가장 오래된 메시지의 messageId}&size=30` |
| C-03 실시간 | 송수신 | **STOMP (4장)** |
| C-04 카페 방문 제안 | 제안 메시지 | STOMP `messageType: "PLACE_PROPOSAL"` + `metadata` (4.4) |
| 읽음 처리 | 방을 보는 중 실시간으로 받은 메시지 읽음 | `POST /chats/rooms/{roomId}/read` |
| 나가기 | 채팅방 퇴장 | `POST /chats/rooms/{roomId}/leave` |

- `unreadCount`는 상대가 보낸 메시지 중 내가 안 읽은 개수입니다. 메시지 **첫 페이지**(`cursorId` 없이)를 조회하면 자동으로 읽음 처리됩니다.
- 채팅방 목록은 **최근 메시지 시각(`lastMessageAt`) 최신순**입니다.
- **나가기:** 내 목록에서 방이 사라지고, 상대 방에는 `SYSTEM` 메시지 "OO님이 채팅방을 나갔습니다."가 실시간으로 갑니다.
- 내 사용자 ID는 `GET /users/me`의 `userId`입니다. 내 메시지 판별은 닉네임보다 `senderId === me.userId`가 안전합니다.

### ⑦ 제휴매장 지도·테마거리 (D-01, D-03, D-11 ~ D-14)
| 동작 | API |
|---|---|
| 지도 첫 화면: 테마거리 점선 구역 | `GET /themes` → 각 항목 `boundary`(꼭짓점 목록), `center`, `name`, `storeCount` |
| 지도 이동 시 화면 안 매장 마커 | `GET /stores?swLat=&swLng=&neLat=&neLng=` |
| 테마거리 선택 → 구역 확대 + 매장 목록 | `GET /themes/{themeStreetId}` → `themeStreet` + `stores` |
| 디저트 필터 (전체/디저트/카페/음료 탭) | 위 두 API에 `dessertType=CAKE` 등 추가 |
| 지역 필터 | `GET /stores?region=대구 중구` |

- `boundary`는 `[{lat, lng}, ...]` 형식이라 카카오맵 `Polygon`·네이버맵 `Polygon` 경로에 바로 넣을 수 있습니다.
- 매장 카드(`stores[]`) 하나가 **마커와 하단 목록에 같이** 쓰입니다: `storeId`, `name`, `latitude`, `longitude`, `dessertTypes`, `representativeMenuName`, `partnerDiscountNotice`, `distanceKm`.
- 지도 영역 4개 값(`swLat`, `swLng`, `neLat`, `neLng`)은 **모두 함께** 보내야 합니다 (하나라도 빠지면 `EX201`).

### ⑧ 매장 상세·예약 및 쿠폰 할인 (D-02 ~ D-05)
| 화면 | 동작 | API |
|---|---|---|
| D-02 매장 상세 | 정보·메뉴·제휴 할인 | `GET /stores/{storeId}` |
| 매장 사용 가능 쿠폰 | 활성 쿠폰 목록 조회 | `GET /stores/{storeId}/coupons` (SHOP-03) |
| 날짜 선택 (달력) | 예약 가능 날짜 | `GET /stores/{storeId}/reservation-dates` (기본 오늘부터 14일) → `available`이 false인 날은 비활성 |
| 시간 선택 | 시간대·잔여 | `GET /stores/{storeId}/reservation-slots?date=2026-11-15` → `available`이 false면 비활성 |
| 예약하기 (쿠폰 포함) | 예약 생성 | `POST /reservations` `{slotId, guestCount(1~10), specialRequest?, couponId?}` → 상태 `PENDING` (RSV-02, RSV-04) |
| 예약 내역 (E-07) | 내 예약 | `GET /reservations/me` |
| 예약 상세 | 예약 1건 | `GET /reservations/{reservationId}` |
| 예약 취소 | 취소 (대기·확정 상태만) | `PATCH /reservations/{reservationId}/cancel` |

- 정원은 **팀 수** 기준입니다 (예약 1건 = 1팀, `guestCount`는 기록용).
- 메뉴는 예약 생성 때가 아니라 **예약 후 ⑧-1 메뉴 주문**에서 고릅니다.
- 지난 시간대는 `available: false`이며 예약 시 `EX302`.
- 결제된 예약을 사용자가 취소하면 주문도 모의 환불(`orderStatus: REFUNDED`)됩니다.

### ⑧-1 메뉴 주문·모의 결제 (D-06 ~ D-09, 4-7)
흐름: 예약 생성 → **메뉴 선택** → **주문 확인(미리보기)** → **모의 결제** → 점주가 준비 단계 변경 (접수 → 준비 중 → 준비 완료)

| 화면 | 동작 | API |
|---|---|---|
| 6-4 메뉴 선택 | 매장 메뉴 (품절 `soldOut`, 탭 분류 `category`: `DESSERT`\|`DRINK`\|`SET`, `categoryLabel`) | `GET /stores/{storeId}` 의 `menus` |
| 7-1 주문 확인 | 금액 계산만 (저장 안 함) | `POST /reservations/{reservationId}/order/preview` `{items: [{menuId, quantity(1~20)}], couponId?}` |
| 7-2 모의 결제 | 결제 승인 + 주문 생성 | `POST /reservations/{reservationId}/order` `{items, couponId?, paymentMethod: CARD\|EASY_PAY, paymentType: COMBINED\|SPLIT}` → 201 |
| 결제 완료·영수증 | 예약의 주문 | `GET /reservations/{reservationId}/order` |
| 마이페이지 주문 내역 | 내 주문 (환불 포함) | `GET /orders/me` |
| 점주 내 매장 | 점주가 관리하는 매장 목록 (첫 화면에서 storeId 결정) | `GET /admin/my-stores` |
| 점주 주문 화면 (4-8) | 매장 주문 메뉴 내역 (점주·관리자) | `GET /admin/stores/{storeId}/orders` |
| 점주 준비 단계 변경 | 준비 중 / 준비 완료 (점주·관리자) | `PATCH /admin/stores/{storeId}/orders/{orderId}/progress` `{step: PREPARING\|READY}` |
| 점주 메뉴 분류 지정 | 메뉴 탭 분류 (점주·관리자) | `PUT /admin/stores/{storeId}/menus/{menuId}/category` `{category: DESSERT\|DRINK\|SET}` |

### ⑨ 하트 구매·마이페이지 및 프리미엄 전용 기능 (E-01 ~ E-07)
| 화면 | 동작 | API |
|---|---|---|
| E-01 마이페이지 | 내 정보 | `GET /users/me` |
| E-03 하트 구매 | 상품 목록 | `GET /hearts/packages` → `[{packageCode, quantity, unitPrice, price}]` (현재 개당 200원) |
| E-03 하트 구매 | 모의 결제 | `POST /hearts/purchase` `{packageCode: "HEARTS_5", paymentMethod: "CARD"}` → `heartCountAfter` |
| 하트 내역 | 충전·차감 이력 | `GET /hearts/history` → `[{id, amount(±), balanceAfter, type, description, createdAt}]` |
| 구매 내역 | 결제 이력 | `GET /hearts/purchases` |
| E-05 나를 좋아요한 사람 | **프리미엄 전용** | `GET /users/me/likes` (일반 회원은 `A002`) |
| E-06 내 프로필 방문자 | **프리미엄 전용** | `GET /users/me/visitors` (PRF-05, 일반 회원은 `A002`) |
| B-04 스킵한 상대 재선택 | **프리미엄 전용** | `GET /matches/history` (MAT-04, 일반 회원은 `A002`) |
| 프리미엄 안내 | 가격·혜택·내 이용 상태 | `GET /premium` → `{premium, expiresAt, monthlyPrice: 9900, periodDays: 30, benefits}` |
| 프리미엄 가입 | 모의 결제 (30일) | `POST /premium/subscribe` `{paymentMethod: "CARD"\|"EASY_PAY"}` → 201 (이미 이용 중이면 `C001`) |
| 프리미엄 결제 내역 | 결제 이력 | `GET /premium/subscriptions` |

### ⑩ AI 맞춤 데이트 장소 추천 (AI-01)
| 화면 | 동작 | API |
|---|---|---|
| C-02 채팅방 상단 AI 버튼 | 맞춤 카페/메뉴 추천 및 카드 발행 | `POST /chats/rooms/{roomId}/ai-recommendation` |

- 로컬 온디바이스 Qwen 2.5 3B (Ollama) 모델이 채팅방 최근 대화와 두 사람의 디저트 취향을 분석하여 최적의 카페와 메뉴를 선정합니다.
- 호출 완료 시 채팅방에 `PLACE_PROPOSAL` 타입 카드 메시지가 실시간 STOMP 브로드캐스트되어 양측 화면에 즉시 노출됩니다.
- Ollama 미기동 시에도 스마트 Fallback 엔진이 100% 정상 작동합니다.

---

## 4. 실시간 채팅 (STOMP) — Swagger에 없음

### 4.1 연결 정보
| 항목 | 값 |
|---|---|
| 엔드포인트 | `wss://desserting-api.duckdns.org/ws` (로컬 `ws://localhost:8080/ws`) |
| 인증 | CONNECT 프레임 헤더 `Authorization: Bearer {accessToken}` (refreshToken 불가) |
| 구독 | `/sub/chat/room/{roomId}` — **해당 방 참여자만** 가능 |
| 발행 | `/pub/chat/message` |

### 4.2 예시 코드
```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: `wss://desserting-api.duckdns.org/ws`,
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 3000,
  onConnect: () => {
    client.subscribe(`/sub/chat/room/${roomId}`, (frame) => {
      const msg = JSON.parse(frame.body);
      appendMessage(msg);
    });
  },
  onStompError: (frame) => {
    // 토큰 만료 또는 권한 실패 시 토큰 재발급 후 재연결
  },
});
client.activate();

// 메시지 보내기
client.publish({
  destination: '/pub/chat/message',
  body: JSON.stringify({ roomId, messageType: 'TEXT', content: '앞산 카페거리 가볼래요?' }),
});
```

### 4.3 새 대화 요청 실시간 알림 안내
- 현재 STOMP 웹소켓에는 별도의 대화 요청 푸시 채널이 연결되어 있지 않습니다.
- 따라서 새 대화 요청 도착 시 홈 배지 및 알림 갱신은 **화면 진입 시나 새로고침 시 `GET /api/v1/matches/requests/received`를 호출**하여 대기 중인 요청 건수를 집계·반영하는 방식을 권장합니다.

---

## 5. 시연용 테스트 계정

| 계정 | 비밀번호 | 특징 |
|---|---|---|
| `demo1@desserting.com` ~ `demo6@desserting.com` | `password123!` | 위치정보 있음 (앞산·수성못·동성로 근처) → 거리 매칭·거리순 테스트용 |
| `minjung@desserting.com` | `test1234!` | 프리미엄 회원 (나를 좋아요한 사람 확인, 스킵 이력 조회) |
| `jihun@desserting.com` 등 목업 유저 | `test1234!` | 일반 회원, 하트 10개 |
| `admin@desserting.com` | `admin1234!` | 관리자 (점주 화면, 예약 상태 변경) — 모든 매장 접근 |
| `owner@anook.com` / `owner@lucid.com` | `owner1234!` | 점주 — **자기 매장만** (아눅 앞산 / 카페 루시드) |

---

## 6. 에러 코드

### 6.1 공통·인증
| code | HTTP | 의미 | 프론트 처리 |
|---|---|---|---|
| `C001` | 400 | 입력값 오류 (`errors[]`에 필드별 사유) | 폼 필드 아래에 `reason` 표시 |
| `C004` | 500 | 서버 오류 | "잠시 후 다시 시도" |
| `A001` | 401 | 인증 실패 (토큰 없음·위조·**만료 포함**) | 2장 재발급 흐름 |
| `A002` | 403 | 권한 없음 (일반 회원 24시간 외 직접 요청, 차단 관계, 프리미엄 전용 등) | 프리미엄 안내 또는 에러 메시지 표시 |
| `A003`, `A004` | 401 | 재발급 실패 | 로그아웃 |

### 6.2 회원·하트·대화 요청·채팅
| code | HTTP | 의미 | 발생 API |
|---|---|---|---|
| `U001` | 404 | 사용자 없음 | 프로필·매칭·요청 |
| `U002` | 409 | 이미 가입된 이메일 | 회원가입 |
| `U003` | 409 | 이미 사용 중인 닉네임 | 회원가입·프로필 수정 |
| `U004` | 400 | 허용되지 않은 아바타 URL | 회원가입·프로필 수정 (`GET /users/avatars` 목록 참조) |
| `H001` | 400 | 하트 부족 (보유 하트 0개) | 대화 요청 → **하트 구매 팝업** |
| `H002` | 409 | 대기 중인 요청 또는 채팅방이 이미 있음 | 대화 요청 |
| `H003` | 404 | 대화 요청 없음 | 수락·거절 |
| `H005` | 409 | 이미 수락·거절된 요청 | 수락·거절 |
| `H006` | 410 | 24시간이 지나 만료된 요청 | 수락·거절 |
| `CH001` | 404 | 채팅방 없음 | 채팅 |
| `CH002` | 403 | 채팅방 참여자가 아님 | 채팅 메시지 조회·구독 |

---

## 7. 최신 상태 현황표
| 항목 | 상태 | 비고 |
|---|:---:|---|
| GCP Compute Engine 서울 리전 호스팅 | ✅ 배포 완료 | `https://desserting-api.duckdns.org` (정식 SSL HTTPS) |
| Swagger UI 연동 | ✅ 배포 완료 | `https://desserting-api.duckdns.org/swagger-ui/index.html` |
| 내가 보낸 대화 요청 목록 (`GET /matches/requests/sent`) | ✅ 구현 완료 | 대기 중인 보낸 요청 목록 및 상대방 프로필 반환 |
| 내가 좋아요한 사람 목록 (`GET /users/me/likes/sent`) | ✅ 구현 완료 | 내가 좋아요 누른 유저 목록 반환 |
| 매칭 후보 응답 상태 플래그 (`requested`, `liked`, `canRequest`) | ✅ 구현 완료 | 버튼 비활성화 및 상태 유지 지원 |
| 기본 아바타 목록 조회 (`GET /users/avatars`) | ✅ 구현 완료 | Vercel 호스팅 8종 아바타 목록 및 검증 |
| 다중 지역 매칭 (OR) & 반경+지역 매칭 (AND) | ✅ 구현 완료 | `AppliedFilter.DISTANCE_AND_REGION` 지원 |
| 4-7 메뉴 주문·모의 결제 (D-06 ~ D-09) | ✅ 구현 완료 | 미리보기·결제·영수증·점주 주문 내역 |
| 4-5 로컬 AI 데이트 추천 (AI-01) | ✅ 구현 완료 | Qwen 2.5 3B (Ollama) 연동 및 스마트 키워드 Fallback |
| 실시간 채팅 수신 & WebSocket | ✅ 동작 완료 | `wss://desserting-api.duckdns.org/ws` |
