# 📢 [공지] 채팅방 '방문 제안' & '예약·결제 내역 공유' 신규 API 배포 안내

안녕하세요 프론트엔드 팀!  
채팅방에서 제휴 매장 방문을 제안하고, 예약 및 결제를 완료한 뒤 채팅방에 내역을 카드 형태로 공유하는 신규 플로우 지원을 위한 백엔드 작업 및 서버 배포가 완료되었습니다.

---

## 🗺️ 변경된 사용자 여정 (User Flow)
```
① 채팅방 [방문 제안] / [예약하기] 버튼
   ↓
② 테마거리 및 제휴 매장 상세 탐색
   ↓
③ 날짜 및 시간대 슬롯 선택 후 예약 생성
   ↓
④ 메뉴 선주문 및 모의 결제 완료
   ↓
⑤ [채팅방에 공유하기] 클릭 시 채팅방에 카드 메시지 실시간 전송!
```

---

## 🚀 신규 및 업데이트 API 명세

### 1. 데이트 제휴 매장 방문 제안 API (신규)
채팅방에서 특정 매장을 상대방에게 가보자고 제안하는 카드 메시지를 발송합니다.

- **HTTP Method / URL**: `POST /api/v1/chats/rooms/{roomId}/propose-place`
- **Request Header**: `Authorization: Bearer {accessToken}`
- **Request Body**:
```json
{
  "storeId": 3,
  "message": "여기 분위기 너무 좋아 보여요! 이번 주말에 같이 가요." // 선택 (생략 시 기본 멘트 발송)
}
```
- **Response / STOMP 수신 메시지 규격**:
  - `messageType`: `"PLACE_PROPOSAL"`
  - `metadata`:
    ```json
    {
      "storeId": 3,
      "storeName": "아눅 앞산",
      "storeAddress": "대구 남구 앞산순환로 459",
      "themeStreetName": "앞산 카페거리",
      "coverImageUrl": "https://..."
    }
    ```
> 💡 **프론트 팁**: 위 메시지를 받으면 카드 UI 내에 **[예약하러 가기]** 버튼을 배치하여 바로 예약 단계로 넘어가게 구현하시면 좋습니다!

---

### 2. 예약 및 결제 내역 채팅방 공유 API (신규)
예약 및 결제를 마친 사용자가 채팅방으로 돌아와 결제 완료 카드를 전송합니다.

- **HTTP Method / URL**: `POST /api/v1/chats/rooms/{roomId}/share-reservation`
- **Request Header**: `Authorization: Bearer {accessToken}`
- **Request Body**:
```json
{
  "reservationId": 15,
  "message": "예약과 주문 결제 완료했어요! 이따 만나요 :)" // 선택 (생략 시 기본 멘트 발송)
}
```
- **Response / STOMP 수신 메시지 규격**:
  - `messageType`: `"RESERVATION_CARD"`
  - `metadata`:
    ```json
    {
      "reservationId": 15,
      "storeId": 3,
      "storeName": "아눅 앞산",
      "storeAddress": "대구 남구 앞산순환로 459",
      "reservationDate": "2026-11-15",
      "reservationTime": "15:00:00",
      "guestCount": 2,
      "status": "CONFIRMED",
      "statusDescription": "예약 확정",
      "totalAmount": 16000,
      "discountAmount": 2000,
      "finalAmount": 14000,
      "orderId": 8,
      "paymentMethod": "CARD",
      "orderedItems": [
        {
          "menuName": "아메리카노",
          "unitPrice": 4500,
          "quantity": 2,
          "lineAmount": 9000
        },
        {
          "menuName": "바닐라 크로플",
          "unitPrice": 7000,
          "quantity": 1,
          "lineAmount": 7000
        }
      ]
    }
    ```
> 💡 **웹소켓(STOMP) 자동 브로드캐스팅**: 해당 API 호출 시 DB 저장과 동시에 `/sub/chat/room/{roomId}` 구독자들에게 실시간으로 전송되므로, 상대방 화면에도 실시간으로 카드 메시지가 뜹니다.

---

### 3. 예약 생성 시 `chatRoomId` 파라미터 연동 (선택 추가)
채팅방에서 예약 플로우를 탔을 경우, 예약 생성 시 `chatRoomId`를 함께 실어 보내주시면 백엔드에 채팅방이 자동 맵핑됩니다.

- **HTTP Method / URL**: `POST /api/v1/reservations`
- **Request Body**:
```json
{
  "slotId": 12,
  "guestCount": 2,
  "specialRequest": "창가 자리 부탁드려요",
  "couponId": 1,
  "chatRoomId": 10 // ✨ 신규 추가 (채팅방에서 진입했을 때 전달)
}
```

---

### 4. 상대방 예약 상세 조회 인가 허용 (개선)
- **URL**: `GET /api/v1/reservations/{reservationId}`
- **개선 내용**:
  - 기존에는 예약한 본인만 조회 가능했으나, 이제 **채팅방에 공유된 예약이라면 상대방 유저가 "상세보기"를 클릭해도 403 차단 없이 정상 조회**됩니다.

---

## 📌 확인용 링크
- **API Base URL**: `https://desserting-api.duckdns.org`
- **Swagger UI**: [https://desserting-api.duckdns.org/swagger-ui/index.html](https://desserting-api.duckdns.org/swagger-ui/index.html) (4-4 실시간 대화 및 채팅방 API 태그 참고)
