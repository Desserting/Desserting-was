# 🍰 디저팅 프론트엔드 API 연동 가이드

> 화면(와이어프레임 ID)별로 **어떤 API를 어떤 순서로 부르는지**, Swagger에 없는 **실시간 채팅(STOMP)**, **에러 코드 처리**를 정리한 문서입니다.
> 요청·응답 필드의 자세한 형식은 Swagger를 기준으로 합니다.
>
> - Swagger UI: `{BASE_URL}/swagger-ui.html`
> - OpenAPI JSON: `{BASE_URL}/v3/api-docs`
> - 기준 코드: `main` `4d2b7ba` + 4-7 메뉴 주문·모의 결제(`feat/order-mock-payment`) — 2026-11-04

---

## 1. 기본 규칙

### 1.1 서버 및 프론트엔드 배포 주소
| 구분 | 접속 주소 | 비고 |
|---|---|---|
| 프론트엔드 (배포) | `https://desserting-web.vercel.app/` | Vercel 공식 배포 웹앱 |
| 백엔드 시연(ngrok) | `https://acetone-suffocate-uncombed.ngrok-free.dev` | 모든 요청에 `ngrok-skip-browser-warning: 69420` 헤더 추가 |
| 백엔드 로컬 | `http://localhost:8080` | README 방법 A/B로 실행 |

- 모든 REST API는 `/api/v1`로 시작합니다.
- 실시간 채팅 웹소켓은 `ws(s)://{호스트}/ws` 입니다 (4장).

### 1.2 인증
- 로그인·회원가입 응답의 `accessToken`을 모든 요청에 `Authorization: Bearer {accessToken}`으로 보냅니다.
- 인증 없이 호출 가능한 API: `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`, `GET /health`, `GET /users`(프로필 목록)

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

- 회원가입 필드: `email`, `password`(6~50자), `nickname`(2~20자), `gender`, `age`(19~99), `region`(예: `대구 남구`), `dessertTypes`(1개 이상), `agreeToTerms`(true 필수)
- 대표 아바타는 첫 번째 디저트 취향으로 **자동 생성**됩니다 (`dessertAvatarUrl`).
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
   - `data.found === true` → `data.candidate` 카드 표시 (`userId`, `nickname`, `age`, `region`, `bio`, `dessertAvatarUrl`, `dessertTags`, `commonDessertTags`, `approxDistanceKm`)
   - `data.found === false` → `data.guide` 문구 표시 (LOC-04 후보 없음 안내)
   - `data.guide`가 있으면(위치 없음 → 지역으로 대체 등) 토스트로 안내
4. 카드 버튼
   - ✕ 다음 상대: **같은 API를 다시 호출** (오늘 본 상대는 자동 제외, 다 보면 `recycled: true`로 처음부터)
   - ♥ 좋아요: `POST /users/{userId}/like` (토글)
   - → 대화 요청: `POST /matches/requests` `{receiverId: candidate.userId}` (⑤)

| 파라미터 | 설명 |
|---|---|
| `matchType` | 필수. `SAME_TASTE` / `DIFFERENT_TASTE` |
| `radiusKm` | 선택. 1~100. 내 좌표가 없으면 활동 지역 기준으로 자동 대체 (`appliedFilter: REGION`) |
| `region` | 선택. 지역명 앞부분 (`대구`, `대구 남구`). `전체`·`대구·경북`·미입력 = 전체 |

- 거리(`approxDistanceKm`)는 개인정보 보호를 위해 **정수 km 올림**으로만 옵니다 (예: `1` = 1km 이내).
- 차단 관계인 사용자, 이성이 아닌 사용자는 나오지 않습니다.

### ④ 프로필 탐색 (B-07)
| 동작 | API |
|---|---|
| 프로필 목록 | `GET /users` |
| 프로필 상세 | `GET /users/{userId}` |
| 좋아요 | `POST /users/{userId}/like` |
| 신고 | `POST /users/{userId}/report` `{reason, description?}` |
| 차단 | `POST /users/{userId}/block?reason=...` |

### ⑤ 하트·대화 요청·수락함 (C-01)
| 화면 | 동작 | API |
|---|---|---|
| 대화 요청 | 요청 보내기 (하트 1개 차감, 프리미엄은 무료). 일반 회원은 **최근 24시간 랜덤 매칭으로 만난 상대**에게만, 프리미엄은 누구에게나 | `POST /matches/requests` `{receiverId}` (조건 밖이면 `A002`) |
| 하트 부족 | `H001` 받으면 하트 구매 팝업 (⑨) | — |
| 수락함 | 받은 요청 목록 | `GET /matches/requests/received` |
| 수락함 | 수락 → 채팅방 생성 | `POST /matches/requests/{requestId}/accept` → `data.chatRoomId` 로 채팅방 이동 |
| 수락함 | 거절 (보낸 사람 하트 환불) | `POST /matches/requests/{requestId}/reject` |

- 24시간 동안 응답이 없으면 자동 만료되고 하트가 환불됩니다.
- 같은 상대에게 대기 중인 요청이 있거나 이미 채팅방이 있으면 `H002`.

### ⑥ 채팅 (C-02, C-03, C-04)
| 화면 | 동작 | API |
|---|---|---|
| C-02 채팅 목록 | 내 채팅방 | `GET /chats/rooms` → `roomId`, `otherUserNickname`, `lastMessagePreview`, `unreadCount` |
| C-03 채팅방 진입 | 이전 메시지 | `GET /chats/rooms/{roomId}/messages?size=30` (최신순) |
| C-03 위로 스크롤 | 더 이전 메시지 | `...?cursorId={가장 오래된 메시지의 messageId}&size=30` |
| C-03 실시간 | 송수신 | **STOMP (4장)** |
| C-04 카페 방문 제안 | 제안 메시지 | STOMP `messageType: "PLACE_PROPOSAL"` + `metadata` (4.4) |
| 나가기 | 채팅방 퇴장 | `POST /chats/rooms/{roomId}/leave` |

- ⚠️ `POST /chats/rooms/direct?targetUserId=` (1:1 방 바로 개설)은 **하트 차감·상대 수락 없이** 방이 만들어집니다. 기획의 "서로 동의한 두 사람만 대화"와 다르므로, 앱 흐름에서는 ⑤의 요청→수락을 사용하세요. (용도는 백엔드와 확정 필요)

### ⑦ 제휴매장 지도·테마거리 (D-01, D-03, D-11 ~ D-14)
| 동작 | API |
|---|---|
| 지도 첫 화면: 테마거리 점선 구역 | `GET /themes` → 각 항목 `boundary`(꼭짓점 목록), `center`, `name`, `storeCount` |
| 지도 이동 시 화면 안 매장 마커 | `GET /stores?swLat=&swLng=&neLat=&neLng=` |
| 테마거리 선택 → 구역 확대 + 매장 목록 | `GET /themes/{themeStreetId}` → `themeStreet` + `stores` |
| 디저트 필터 (전체/디저트/카페/음료 탭) | 위 두 API에 `dessertType=CAKE` 등 추가 |
| 지역 필터 | `GET /stores?region=대구 중구` |

- `boundary`는 `[{lat, lng}, ...]` 형식이라 카카오맵 `Polygon`·네이버맵 `Polygon` 경로에 바로 넣을 수 있습니다 (점선 스타일은 지도 SDK 옵션).
- 매장 카드(`stores[]`) 하나가 **마커와 하단 목록에 같이** 쓰입니다: `storeId`, `name`, `latitude`, `longitude`, `dessertTypes`, `representativeMenuName`, `partnerDiscountNotice`, `distanceKm`.
- `distanceKm`은 `lat`·`lng`를 보내면 그 위치 기준, 안 보내면 저장된 내 위치 기준이며, 기준이 있으면 가까운 순으로 정렬됩니다.
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
- 메뉴는 예약 생성 때가 아니라 **예약 후 ⑧-1 메뉴 주문**에서 고릅니다 (`orderMenus` 같은 필드는 없음).
- `couponId`는 예약에 **기억만** 합니다. 응답의 `couponName`은 채워지지만 `totalAmount`·`discountAmount`·`finalAmount`는 **주문 결제 전까지 0**입니다. 최소 주문 금액 확인과 할인은 주문 때 합니다.
- 다른 매장·사용 중지·만료된 쿠폰이면 `EX305`.
- 지난 시간대는 `available: false`이며 예약 시 `EX302`.
- 예약 상태(`PREPARING`, `COMPLETED` 등)는 점주가 바꿉니다. 사용자 화면은 `GET /reservations/me`를 다시 불러 반영합니다.
- 결제된 예약을 사용자가 취소하면 주문도 모의 환불(`orderStatus: REFUNDED`)됩니다.

### ⑧-1 메뉴 주문·모의 결제 (D-06 ~ D-09, 4-7)
흐름: 예약 생성 → **메뉴 선택** → **주문 확인(미리보기)** → **모의 결제** → 점주가 준비 상태 변경

| 화면 | 동작 | API |
|---|---|---|
| 6-4 메뉴 선택 | 매장 메뉴 (품절 `isSoldOut`) | `GET /stores/{storeId}` 의 `menus` |
| 7-1 주문 확인 | 금액 계산만 (저장 안 함) | `POST /reservations/{reservationId}/order/preview` `{items: [{menuId, quantity(1~20)}], couponId?}` |
| 7-2 모의 결제 | 결제 승인 + 주문 생성 | `POST /reservations/{reservationId}/order` `{items, couponId?, paymentMethod: CARD\|EASY_PAY, paymentType: COMBINED\|SPLIT}` → 201 |
| 결제 완료·영수증 | 예약의 주문 | `GET /reservations/{reservationId}/order` |
| 마이페이지 주문 내역 | 내 주문 (환불 포함) | `GET /orders/me` |
| 점주 주문 화면 (4-8) | 매장 주문 메뉴 내역 (점주·관리자) | `GET /admin/stores/{storeId}/orders` |

```jsonc
// POST /reservations/10/order  { "items": [{"menuId":1,"quantity":2},{"menuId":2,"quantity":1}], "paymentMethod":"CARD", "paymentType":"SPLIT" }
{ "orderId": 1, "reservationId": 10, "items": [ {"menuId":1,"menuName":"바스크 치즈케이크","unitPrice":8000,"quantity":2,"lineAmount":16000}, ... ],
  "totalAmount": 22800, "couponName": "디저팅 첫 데이트 2,000원 할인", "discountAmount": 2000, "finalAmount": 20800,
  "paymentType": "SPLIT", "shareAmounts": [10400, 10400], "paymentMethod": "CARD", "paymentKey": "MOCK-ORD-...",
  "orderStatus": "PAID", "reservationStatus": "PENDING", "paidAt": "2026-11-04T10:48:41", "refundedAt": null }
```
- 예약 **대기·확정** 상태에서만, 예약 1건당 **1번** 주문할 수 있습니다 (다시 결제하면 `EX402`).
- 같은 `menuId`를 여러 줄로 보내면 수량이 합쳐집니다. 메뉴명·단가는 주문 시점 값으로 저장됩니다.
- `couponId`를 생략하면 **예약할 때 고른 쿠폰**을 씁니다. 다른 쿠폰 ID를 보내면 그 쿠폰으로 바뀝니다.
- 할인액이 메뉴 합계보다 크면 합계까지만 할인합니다 (`finalAmount`는 0 미만이 되지 않음).
- `SPLIT`(개별 결제)은 예약 인원(`guestCount`, 2명 이상)으로 나누며 1원 단위 나머지는 앞사람이 냅니다 → `shareAmounts`. `COMBINED`는 1명이 전액.
- 결제 후 예약의 `totalAmount`·`discountAmount`·`finalAmount`가 함께 갱신되어 예약 내역·점주 화면에 보입니다.
- 매장 준비 단계(접수 → 준비 중 → 방문 완료)는 주문이 아닌 **예약 상태(`reservationStatus`)**를 보면 됩니다.
- 로컬 시드: 제휴매장마다 쿠폰 2종 (`2,000원 할인 / 1만원 이상`, `3,000원 할인 / 2만원 이상`).

### ⑨ 하트 구매·마이페이지 및 프리미엄 전용 기능 (E-01 ~ E-07)
| 화면 | 동작 | API |
|---|---|---|
| E-01 마이페이지 | 내 정보 | `GET /users/me` |
| E-03 하트 구매 | 상품 목록 | `GET /hearts/packages` → `[{packageCode, quantity, unitPrice, price}]` (현재 개당 200원) |
| E-03 하트 구매 | 모의 결제 | `POST /hearts/purchase` `{packageCode: "HEARTS_5", paymentMethod: "CARD"}` → `heartCountAfter` |
| 하트 내역 | 충전·차감 이력 | `GET /hearts/history` |
| 구매 내역 | 결제 이력 | `GET /hearts/purchases` |
| E-05 나를 좋아요한 사람 | **프리미엄 전용** | `GET /users/me/likes` (일반 회원은 `A002`) |
| E-06 내 프로필 방문자 | **프리미엄 전용** | `GET /users/me/visitors` (PRF-05, 일반 회원은 `A002`) |
| B-04 스킵한 상대 재선택 | **프리미엄 전용** | `GET /matches/history` (MAT-04, 일반 회원은 `A002`) |
| 프리미엄 안내 | 가격·혜택·내 이용 상태 | `GET /premium` → `{premium, expiresAt, monthlyPrice: 9900, periodDays: 30, benefits}` |
| 프리미엄 가입 | 모의 결제 (30일) | `POST /premium/subscribe` `{paymentMethod: "CARD"\|"EASY_PAY"}` → 201 (이미 이용 중이면 `C001`) |
| 프리미엄 결제 내역 | 결제 이력 | `GET /premium/subscriptions` |

- 가격은 프론트에 하드코딩하지 말고 `GET /hearts/packages` 값을 표시하세요 (가격 정책 변경 대비).
- 프리미엄 회원은 하트를 쓰지 않아 구매가 막혀 있습니다 (`C001`).
- 프리미엄 가입 직후부터 `GET /users/me`의 `isPremium`이 `true`입니다. 기간이 끝나면 자동으로 일반 회원이 됩니다 (시드 프리미엄 계정은 만료 없음).
- 프로필 직접 선택(ProfileExplore)에서 일반 회원이 요청하면 `A002` → 프리미엄 안내 화면으로 보내 주세요.

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
| 엔드포인트 | `ws://localhost:8080/ws` (순수 WebSocket, `@stomp/stompjs` 권장) |
| 인증 | CONNECT 프레임 헤더 `Authorization: Bearer {accessToken}` (refreshToken 불가) |
| 구독 | `/sub/chat/room/{roomId}` — **해당 방 참여자만** 가능 |
| 발행 | `/pub/chat/message` |

### 4.2 예시 코드
```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: `${WS_BASE_URL}/ws`,                       // 예: ws://localhost:8080/ws
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 3000,
  onConnect: () => {
    client.subscribe(`/sub/chat/room/${roomId}`, (frame) => {
      const msg = JSON.parse(frame.body);                // 4.3 형식
      appendMessage(msg);
    });
  },
  onStompError: (frame) => {
    // 연결 인증 실패, 방 참여자가 아닌데 구독한 경우 등.
    // frame.headers['message'] 는 서버 내부 문구라 사용자에게 보여주지 말고,
    // 토큰 재발급(2장) 후 재연결 → 그래도 실패하면 "채팅방에 들어갈 수 없습니다" 안내
  },
});
client.activate();

// 메시지 보내기
client.publish({
  destination: '/pub/chat/message',
  body: JSON.stringify({ roomId, messageType: 'TEXT', content: '앞산 카페거리 가볼래요?' }),
});
```

### 4.3 메시지 형식
보내는 메시지:
```json
{ "roomId": 10, "messageType": "TEXT", "content": "안녕하세요!", "metadata": null }
```
받는 메시지 (구독 콜백, 이전 메시지 조회 API와 같은 형식):
```json
{
  "messageId": "6547...", "roomId": 10, "senderId": 1, "senderNickname": "딸기쇼트케이크",
  "senderAvatarUrl": "...", "messageType": "TEXT", "content": "안녕하세요!",
  "metadata": null, "createdAt": "2026-11-03T21:00:00.123"
}
```
- `messageType`: `TEXT`, `IMAGE`, `PLACE_PROPOSAL`, `SYSTEM`
- **보낸 사람도 같은 방을 구독 중이면 자기 메시지를 받습니다.** 화면에 직접 추가하지 말고 구독으로 받은 메시지로 그리면 중복이 생기지 않습니다.

### 4.4 카페 방문 제안 (C-04, `PLACE_PROPOSAL`)
`metadata`는 자유 형식이라 아래 형식으로 **프론트·백엔드가 맞추는 것을 제안**합니다.
```json
{
  "roomId": 10, "messageType": "PLACE_PROPOSAL", "content": "아눅 앞산에서 내일 3시 어때요?",
  "metadata": { "storeId": 1, "storeName": "아눅 앞산 (a.nook Apsan)", "proposedDate": "2026-11-04", "proposedTime": "15:00" }
}
```
받는 쪽은 `storeId`로 `GET /stores/{storeId}` 상세나 예약 화면으로 바로 이동시킬 수 있습니다.

### 4.5 주의
- 실시간 수신은 **PR #3이 머지된 서버**에서 동작합니다. 그 이전 버전 서버에서는 메시지가 저장만 되고 실시간으로 오지 않으니, 동작하지 않으면 서버 버전을 먼저 확인하세요.
- ⚠️ **메시지 전송 실패는 클라이언트에 알려지지 않습니다.** 차단 관계, 나간 방, 참여자 아님 등으로 서버가 거절해도 에러 프레임이 오지 않고 메시지만 사라집니다. 일정 시간 안에 구독으로 내 메시지가 돌아오지 않으면 "전송 실패"로 표시하는 처리를 권장합니다.
- 연결 인증 실패(잘못된·만료된 토큰, refreshToken 사용)와 참여자가 아닌 방 구독은 STOMP ERROR 프레임으로 옵니다. ERROR 프레임의 `message`는 서버 내부 문구(`Failed to send message to ...`)라 원인을 구분할 수 없으므로, 토큰 재발급(2장) 후 재연결하는 방식으로 처리하세요.
- 채팅방 목록의 마지막 메시지·안 읽은 수는 실시간 갱신되지 않으므로 목록 화면 진입 시 `GET /chats/rooms`를 다시 호출합니다.

---

## 5. 시연용 테스트 계정 (로컬 시드 데이터)

| 계정 | 비밀번호 | 특징 |
|---|---|---|
| `demo1@desserting.com` ~ `demo6@desserting.com` | `password123!` | 위치정보 있음 (앞산·수성못·동성로 근처) → 거리 매칭·거리순 테스트용 |
| `minjung@desserting.com` | `test1234!` | 프리미엄 회원 (나를 좋아요한 사람 확인) |
| `jihun@desserting.com` 등 목업 유저 | `test1234!` | 위치정보 없음 (지역 기준 매칭) |
| `admin@desserting.com` | `admin1234!` | 관리자 (점주 화면, 예약 상태 변경) |

- 테마거리 3곳(앞산 카페거리, 동성로 통신골목 카페거리, 김광석 다시그리기길 카페거리), 제휴매장 5곳, 앞으로 14일치 예약 시간대(14·16·18·20시, 각 4팀)가 들어 있습니다.

---

## 6. 에러 코드

### 6.1 공통·인증
| code | HTTP | 의미 | 프론트 처리 |
|---|---|---|---|
| `C001` | 400 | 입력값 오류 (`errors[]`에 필드별 사유) | 폼 필드 아래에 `reason` 표시 |
| `C002` | 405 | 허용되지 않은 메서드 | 개발 중 확인 |
| `C004` | 500 | 서버 오류 | "잠시 후 다시 시도" |
| `C005` | 409 | 같은 데이터를 동시에 바꾸려다 충돌 (예: 점주 예약 상태 변경과 사용자 예약이 같은 시간대에 동시 발생) | 최신 데이터를 다시 불러온 뒤 재시도 안내 |
| `A001` | 401 | 인증 실패 (토큰 없음·위조·**만료 포함**) | 2장 재발급 흐름 |
| `A002` | 403 | 권한 없음 (프리미엄 전용, 관리자 전용, 차단 관계 등) | 프리미엄 안내 또는 `message` 표시 |
| `A003`, `A004` | 401 | 재발급 실패 (만료·위조된 refreshToken, 또는 accessToken을 넣은 경우) | 로그아웃 |

### 6.2 회원·하트·대화 요청·채팅
| code | HTTP | 의미 | 발생 API |
|---|---|---|---|
| `U001` | 404 | 사용자 없음 | 프로필·매칭·요청 |
| `U002` | 409 | 이미 가입된 이메일 | 회원가입 |
| `U003` | 409 | 이미 사용 중인 닉네임 | 회원가입·프로필 수정 |
| `H001` | 400 | 하트 부족 | 대화 요청 → **하트 구매 팝업** |
| `H002` | 409 | 대기 중인 요청 또는 채팅방이 이미 있음 | 대화 요청 |
| `H003` | 404 | 대화 요청 없음 | 수락·거절 |
| `H004` | 400 | 자기 자신에게 요청 | 대화 요청·1:1 방 |
| `CH001` | 404 | 채팅방 없음 | 채팅 |
| `CH002` | 403 | 채팅방 참여자가 아님 | 채팅 메시지 조회·구독 |

### 6.3 지도·매장·예약·주문 (4-2, 4-5, 4-6, 4-7)
| code | HTTP | 의미 | 발생 API |
|---|---|---|---|
| `EX001` | 400 | 쿼리 파라미터·요청 본문 형식 오류 (예: `matchType=X`, `date` 누락) | 매칭·지도·매장·예약 |
| `EX101` | 400 | 매칭 반경이 1~100km 밖 | 랜덤 매칭 |
| `EX201` | 400 | 지도 영역 4개 값 중 일부 누락 또는 남서>북동 | 매장 검색 |
| `EX301` | 400 | 예약 날짜 조회 기간 오류 (최대 31일) | 예약 가능 날짜 |
| `EX302` | 400 | 이미 지난 시간대 | 예약 생성 |
| `EX303` | 409 | 같은 시간대에 이미 예약함 | 예약 생성 |
| `EX304` | 400 | 취소할 수 없는 상태 (준비 중·완료·이미 취소) | 예약 취소 |
| `EX305` | 400 | 쓸 수 없는 쿠폰 (다른 매장·사용 중지·만료·최소 주문 금액 미달) | 예약 생성, 주문 미리보기·결제 |
| `EX401` | 400 | 주문할 수 없는 예약 상태 (준비 중·완료·취소) | 주문 미리보기·결제 |
| `EX402` | 409 | 이미 결제한 예약 | 주문 결제 |
| `EX403` | 400 | 예약한 매장의 메뉴가 아님 | 주문 미리보기·결제 |
| `EX404` | 409 | 품절 메뉴 포함 → **메뉴 다시 불러오기** | 주문 미리보기·결제 |
| `EX405` | 404 | 주문 없음 (아직 결제 전이거나 본인 주문 아님) | 주문 조회 |
| `EX406` | 400 | 1인 예약은 개별 결제(`SPLIT`) 불가 | 주문 결제 |
| `T001` | 404 | 테마거리 없음 | 테마거리 선택 |
| `S001` | 404 | 매장 없음 또는 제휴 해지 | 매장 상세·예약 |
| `R001` | 409 | 시간대 마감 | 예약 생성 → **시간 목록 새로고침** |
| `R002` | 404 | 예약 없음 (본인 예약이 아님 포함) | 예약 상세·취소 |
| `R003` | 404 | 예약 시간대 없음 | 예약 생성 |

---

## 7. 아직 없는 기능 / 확인 필요 (최신 상태)
| 항목 | 상태 | 비고 |
|---|:---:|---|
| 4-7 메뉴 주문·모의 결제 (D-06 ~ D-09) | ✅ 구현 완료 | ⑧-1 참고. 미리보기·결제·영수증·점주 주문 내역, 합산/개별 결제, 쿠폰 할인 |
| 점주 정산 금액 (4-8) | ⚠️ 확인 필요 | 정산이 할인 전 `totalAmount` 기준으로 집계됨 (4-8 담당 확인 중) |
| 4-1 방문자 확인 (PRF-05) & 4-2 스킵 상대 (MAT-04) | ✅ 구현 완료 | 프리미엄 전용 `GET /users/me/visitors`, `GET /matches/history` 제공 |
| 4-5 로컬 AI 데이트 추천 (AI-01) | ✅ 구현 완료 | `POST /chats/rooms/{roomId}/ai-recommendation` (Qwen 2.5 3B 연동) |
| 시연 서버(ngrok) 주소 | ✅ 연동 완료 | `https://acetone-suffocate-uncombed.ngrok-free.dev` |
| 공식 프론트엔드(Vercel) 배포 | ✅ 배포 완료 | `https://desserting-web.vercel.app/` (CORS 허용 완료) |
| 실시간 채팅 수신 & WebSocket | ✅ 동작 완료 | `/pub/chat/message`, `/sub/chat/room/{roomId}` 정상 동작 |
| 외부 PG사(토스/포트원) 실제 카드 결제 | ❌ 미구현 | 해커톤 기획상 모의 결제 시스템으로 대체 운영 |
| 실시간 모바일 푸시 알림 (FCM) | ❌ 미구현 | 모바일 앱 백그라운드 푸시 알림 (웹소켓 실시간 수신으로 대체) |
| 개인 프로필 사진 업로드 | ❌ 미구현 | 기획상 외모 지양 및 디저트 취향 아바타(6종) 중심 매칭 |
| 동성 카페 친구 매칭 | ❌ 미구현 | 기획상 초기엔 이성 소개팅 및 데이트 위주로 집중 운영 |
