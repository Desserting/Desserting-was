# 🍰 디저팅 (Desserting) — 백엔드

> **2026 단디톤(Dandi-thon) 출품작**  
> *"디저트 취향을 통한 만남부터 대구·경북 제휴매장 실제 데이트까지 원스톱 연결"*

---

## 🛠 기술 스택 (Tech Stack)

| 분류 | 기술 / 라이브러리 | 버전 | 주요 역할 |
| :--- | :--- | :--- | :--- |
| **Language** | **Java** | 21 (LTS) | Virtual Threads 기반 동시성 I/O 처리 |
| **Framework** | **Spring Boot** | 3.3.5 | REST API, STOMP 실시간 웹소켓, 멀티 데이터소스 제어 |
| **Build Tool** | **Gradle** | 8.10.2 | Gradle Wrapper(`gradlew`) 기반 빌드 자동화 |
| **RDB** | **PostgreSQL** | 16-alpine | 회원, 디저트 프로필, 매칭, 매장, 예약 영구 저장 |
| **NoSQL** | **MongoDB** | 7.0 | 실시간 대용량 채팅 메시지 적재 및 페이징 조회 |
| **In-Memory** | **Redis** | 7.2-alpine | STOMP Pub/Sub 브로드캐스팅, Presence(접속 상태) 캐시 |
| **Security** | **Spring Security + JJWT** | JJWT 0.12.6 | Stateless JWT Access(1시간)/Refresh(14일) 토큰 인가 |
| **API Docs** | **SpringDoc OpenAPI** | 2.6.0 | Swagger UI 및 프론트 `openapi-typescript` 타입 동기화 |
| **Container** | **Docker & Docker Compose** | - | 3종 DB(Postgres, Mongo, Redis) 로컬 일괄 오케스트레이션 |

---

## 📋 사전 요구사항 (Prerequisites)

- **Java 21 (LTS)**: Gradle Java Toolchain을 통해 자동 관리됩니다.
- **Docker & Docker Compose** (또는 [OrbStack](https://orbstack.dev/)): 3종 DB 컨테이너 실행에 필요합니다.

---

## 🚀 서버 구동 방법 (Quick Start)

### 1단계: 환경 변수 설정
프로젝트 루트 디렉토리에서 환경 변수 예시 파일을 복사하여 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

### 2단계: 3종 DB 컨테이너 실행 (Docker)
PostgreSQL(5432), MongoDB(27017), Redis(6379)를 백그라운드 컨테이너로 일괄 실행합니다.

```bash
docker compose up -d
```

> 컨테이너가 정상 실행되었는지 확인합니다:
> ```bash
> docker compose ps
> ```

### 3단계: 백엔드 애플리케이션 빌드 및 실행

**터미널에서 실행할 경우:**
```bash
./gradlew bootRun
```

**IntelliJ IDEA에서 실행할 경우:**
- `src/main/java/com/desserting/backend/DessertingApplication.java` 파일을 열고 **Run (Ctrl + R / Shift + F10)** 실행

### 4단계: 서버 정상 구동 확인
브라우저 또는 터미널(curl)에서 헬스 체크 엔드포인트를 호출합니다:
- **URL**: `http://localhost:8080/api/v1/health`
- **응답 예시**:
  ```json
  {
    "success": true,
    "message": "서버가 정상 동작 중입니다.",
    "data": {
      "status": "UP",
      "profile": "local",
      "serverTime": "2026-11-03T11:50:00"
    },
    "timestamp": "2026-11-03T11:50:00"
  }
  ```

---

## 📖 API 문서 및 스웨거(Swagger) 테스트

서버 구동 후 웹 브라우저에서 아래 주소로 접속하면 인터랙티브 API 명세서를 확인할 수 있습니다:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### 스웨거를 통한 기본 테스트 흐름:
1. **서버 확인**: `00. Health Check` → `GET /api/v1/health` → `Try it out` → `Execute`
2. **회원가입**: `01. Auth` → `POST /api/v1/auth/signup`
   - 샘플 요청 바디:
     ```json
     {
       "email": "test@desserting.com",
       "password": "password123!",
       "nickname": "딸기쇼트케이크",
       "gender": "FEMALE",
       "age": 25,
       "region": "대구 남구",
       "dessertTypes": ["CAKE", "COFFEE"],
       "agreeToTerms": true
     }
     ```
   - 응답 결과에서 `accessToken` 값을 복사합니다.
3. **JWT 인증 등록**:
   - 스웨거 우측 상단 **`Authorize (자물쇠)`** 버튼 클릭
   - 복사한 `accessToken` 붙여넣기 (접두사 `Bearer ` 없이 토큰 값만 입력) → `Authorize` 클릭
4. **내 프로필 조회**: `02. User` → `GET /api/v1/users/me` → `Execute`
   - 기본 무료 하트 5개(`heartCount: 5`) 및 자동 생성된 디저트 아바타 확인
5. **프로필 수정**: `02. User` → `PATCH /api/v1/users/me/profile`
   - 닉네임, 한 줄 소개, 디저트 취향 태그 변경 테스트

---

## 🧪 자동화 단위 테스트 실행

DB나 외부 인프라 연결 없이 로직을 즉시 검증할 수 있습니다:

```bash
./gradlew test
```

- 테스트 보고서 확인: 브라우저에서 `build/reports/tests/test/index.html` 파일을 열어 확인 가능

---

## 📁 주요 프로젝트 문서

- 📄 [백엔드 구현 기획서](docs/백엔드_구현_기획서.md)
- 📄 [백엔드 버전 및 협업 가이드](docs/백엔드_버전_및_협업_가이드.md)
