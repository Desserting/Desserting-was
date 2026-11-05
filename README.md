# 🍰 디저팅 (Desserting) — 백엔드

> **2026 단디톤(Dandi-thon) 출품작**  
> *"디저트 취향을 통한 만남부터 대구·경북 제휴매장 실제 데이트까지 원스톱 연결"*

---

## 🌐 주요 서비스 접속 링크 (Quick Links)

| 서비스 | 환경 | 접속 주소 | 설명 |
| :--- | :---: | :--- | :--- |
| **🎨 공식 프론트엔드 (Vercel)** | **배포** | **[https://desserting-web.vercel.app/](https://desserting-web.vercel.app/)** | 디저팅 공식 배포 프론트엔드 웹 애플리케이션 |
| **☁️ 운영 백엔드 API (Google Cloud)** | **운영** | **`https://desserting-api.duckdns.org`** | 24/7 무중단 운영 API 서버 (서울 리전, HTTPS 적용) |
| **📖 스웨거 (Swagger UI)** | 운영 | **[Swagger UI (Live)](https://desserting-api.duckdns.org/swagger-ui/index.html)** | 운영 환경 인터랙티브 API 명세서 및 실시간 테스트 |
| | 로컬 | **[Swagger UI (Local)](http://localhost:8080/swagger-ui.html)** | 로컬 환경 Swagger UI |

---

## 🤖 로컬 AI (Ollama Qwen 2.5 3B) 구동 가이드

디저팅 백엔드는 **로컬 On-Device LLM (Qwen 2.5 3B)**을 활용하여 채팅방 대화 맥락과 디저트 취향을 분석하고, 대구 제휴 카페와 대표 메뉴를 실시간으로 추천합니다 (`AI-01`).

### 1. Docker Compose 일괄 구동 (권장)
백엔드 `docker-compose.yml`에 `ollama` 서비스가 기본 포함되어 있어, 별도 설치 없이 단일 명령어로 실행됩니다:
```bash
docker compose up -d
# 최초 1회 모델 다운로드
docker exec desserting-ollama ollama pull qwen2.5:3b
```

### 2. Mac 로컬 직접 설치 시
```bash
# 1. Ollama 설치 (Mac Homebrew)
brew install ollama

# 2. Ollama 백그라운드 서비스 시작
brew services start ollama

# 3. 한국어 성능이 뛰어난 Qwen 2.5 3B 모델 다운로드 (약 1.9GB)
ollama pull qwen2.5:3b
```

### 3. 구동 상태 확인
```bash
curl http://localhost:11434/api/tags
# {"models":[{"name":"qwen2.5:3b", ...}]} 응답 확인
```

> [!NOTE]
> **무중단 스마트 Fallback 지원**: Ollama가 미기동 상태이거나 지연/타임아웃이 발생해도 백엔드의 내장 **스마트 키워드 Fallback 엔진**이 즉시 작동하여 대화 키워드(카페명, 메뉴명, 지역명)와 유저 프로필 취향을 분석해 100% 정상 추천을 보장합니다.

---

## 👥 시연 및 테스트용 목업 계정 (1-Click 계정)

서버 기동 시 `local`, `dev` 프로필에서 아래 목업 데이터가 자동 시딩됩니다:

| 구분 | 이메일 | 비밀번호 | 닉네임 | 권한 / 상태 |
| :--- | :--- | :--- | :--- | :--- |
| **일반 유저 1** | `minjung@desserting.com` | `test1234!` | 초코크로와상 | ROLE_USER, **프리미엄(방문자/스킵조회 가능)**, 하트 50개 |
| **일반 유저 2** | `jihun@desserting.com` | `test1234!` | 드립마니아 | ROLE_USER, 일반회원, 하트 10개 |
| **일반 유저 3** | `soyeon@desserting.com` | `test1234!` | 딸기푸딩 | ROLE_USER, 일반회원, 하트 10개 |
| **데모 유저 1~6** | `demo1~6@desserting.com` | `password123!` | 딸기쇼트, 드립마니아 등 | ROLE_USER, 위치/취향 기반 매칭 테스트용 계정 |
| **점주 (루시드)** | `owner@lucid.com` | `owner1234!` | 루시드점주 | ROLE_STORE_OWNER, 카페 루시드 (동성로) |
| **점주 (아눅)** | `owner@anook.com` | `owner1234!` | 아눅점주 | ROLE_STORE_OWNER, 아눅 앞산 (앞산순환로) |
| **플랫폼 관리자** | `admin@desserting.com` | `admin1234!` | 디저팅운영자 | ROLE_ADMIN, 하트 999개 |

---

## 🛠 기술 스택 (Tech Stack)

| 분류 | 기술 / 라이브러리 | 버전 | 주요 역할 |
| :--- | :--- | :--- | :--- |
| **Cloud Infra** | **GCP Compute Engine + Caddy** | Ubuntu 24.04 | 24/7 서울 리전 무중단 클라우드 호스팅 및 자동 SSL(HTTPS) |
| **Language** | **Java** | 21 (LTS) | Virtual Threads 기반 동시성 I/O 처리 |
| **Framework** | **Spring Boot** | 3.3.5 | REST API, STOMP 실시간 웹소켓, 멀티 데이터소스 제어 |
| **AI / LLM** | **Ollama (Qwen 2.5 3B)** | 3.1B 파라미터 | 로컬 온디바이스 데이트 코스 & 메뉴 맞춤 추천 (AI-01) |
| **RDB** | **PostgreSQL** | 16-alpine | 회원, 디저트 프로필, 매칭, 쿠폰, 매장, 예약 영구 저장 |
| **NoSQL** | **MongoDB** | 7.0 | 실시간 대용량 채팅 메시지 적재 및 커서 페이징 |
| **In-Memory** | **Redis** | 7.2-alpine | STOMP Pub/Sub 브로드캐스팅, 스킵 상대 보관(ZSet), RTR 토큰 관리 |
| **Security** | **Spring Security + JJWT** | JJWT 0.12.6 | Stateless JWT Access(1시간)/Refresh(14일) 토큰 인가 및 RTR 블랙리스트 |
| **API Docs** | **SpringDoc OpenAPI** | 2.6.0 | Swagger UI 및 프론트 `openapi-typescript` 타입 동기화 |
| **Container** | **Docker & Docker Compose** | v29.9 / v5.7 | 백엔드, 3종 DB 및 Ollama 일괄 오케스트레이션 |

---

## 🚀 서버 구동 방법 (Quick Start)

### 사전 준비: 환경 변수 파일 생성
```bash
cp .env.example .env
```

### 방법 A. Docker로 전체 서버 및 DB 일괄 실행 (추천 / 원클릭 실행)
Spring Boot 백엔드 서버와 3종 DB(PostgreSQL, MongoDB, Redis), Ollama AI 컨테이너를 모두 Docker로 빌드 및 실행합니다.

```bash
# 전체 서비스 빌드 및 백그라운드 구동
docker compose up --build -d

# 구동 상태 확인 (5개 컨테이너가 Up 상태인지 확인)
docker compose ps

# 백엔드 서버 실시간 로그 확인
docker compose logs -f backend
```

---

### 방법 B. DB만 Docker로 띄우고 백엔드는 로컬에서 실행 (개발/디버깅 모드)
코드 수정이 잦거나 IntelliJ 브레이크포인트 디버깅을 사용할 때 권장합니다.

```bash
# 1. 3종 DB 컨테이너만 실행
docker compose up -d postgres mongodb redis ollama

# 2. 백엔드 로컬 실행
./gradlew bootRun
# 또는 IntelliJ에서 DessertingApplication.java Run
```

---

## 📖 스웨거(Swagger) 인증 및 테스트 방법

1. 브라우저에서 **[https://desserting-api.duckdns.org/swagger-ui/index.html](https://desserting-api.duckdns.org/swagger-ui/index.html)** 접속
2. **로그인 토큰 발급**:
   * `POST /api/v1/auth/login` 실행 (예: `demo1@desserting.com` / `password123!`)
   * 반환된 `data.accessToken` 복사
3. **인증 등록**:
   * 우측 상단 **`Authorize 🔓`** 클릭 ➔ 단일 입력창(`bearerAuth`)에 따옴표(`"`)와 `Bearer ` 제외하고 **순수 토큰만 붙여넣기**
   * [Authorize] ➔ [Close] 클릭
4. **주요 API 테스트**:
   * **4-2. 매칭**: 랜덤 매칭 조회 (`GET /api/v1/matches/random`), 스킵 이력 (`GET /api/v1/matches/history`)
   * **4-3. 대화 요청**: 보낸 요청 목록 (`GET /api/v1/matches/requests/sent`), 받은 요청 목록 (`GET /api/v1/matches/requests/received`)
   * **4-4. 소셜**: 내가 좋아요한 목록 (`GET /api/v1/users/me/likes/sent`), 나를 좋아요한 목록 (`GET /api/v1/users/me/likes`)
   * **4-5. 채팅/AI**: AI 데이트 카페 추천 (`POST /api/v1/chats/rooms/{roomId}/ai-recommendation`)
   * **4-6/4-8. 매장/쿠폰**: 매장 쿠폰 조회 (`GET /api/v1/stores/{storeId}/coupons`), 점주 쿠폰 등록 및 할인 예약

---

## 🧪 자동화 테스트 검증

```bash
# 전체 단위 및 통합 테스트 실행
./gradlew test --rerun-tasks
```

---

## 📁 주요 프로젝트 문서

- 📄 [기능 구현 및 미구현 분석 보고서 (최신)](docs/feature_implementation_analysis_report.md)
- 📄 [프론트엔드 API 연동 가이드](docs/frontend_api_integration_guide.md)
- 📄 [백엔드 구현 기획서](docs/backend_implementation_plan.md)
- 📄 [백엔드 버전 및 협업 가이드](docs/backend_version_guide.md)
