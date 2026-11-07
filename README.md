# 디저팅 (Desserting) — 백엔드

> **2026 단디톤(Dandi-thon) 출품작**  
> *"디저트 취향을 통한 만남부터 대구·경북 제휴매장 실제 데이트까지 원스톱 연결"*

---

## 서비스 접속 링크

| 서비스 | 접속 주소 |
| :--- | :--- |
| **디저팅** | **https://desserting-web.vercel.app/** |
| **API** | **https://desserting-api.duckdns.org** |
| **API Docs** | **https://desserting-api.duckdns.org/swagger-ui/index.html** |

---

## 테스트용 목업 계정

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

## 기술 스택 (Tech Stack)

| 분류 | 기술 / 라이브러리 | 버전 |
| :--- | :--- | :--- |
| **Cloud Infra** | **GCP Compute Engine + Caddy** | Ubuntu 24.04 |
| **Language** | **Java** | 21 (LTS) |
| **Framework** | **Spring Boot** | 3.3.5 |
| **AI / LLM** | **Ollama (Qwen 2.5 3B)** | 3.1B 파라미터 |
| **RDB** | **PostgreSQL** | 16-alpine |
| **NoSQL** | **MongoDB** | 7.0 |
| **In-Memory** | **Redis** | 7.2-alpine |
| **Security** | **Spring Security + JJWT** | JJWT 0.12.6 |
| **API Docs** | **SpringDoc OpenAPI** | 2.6.0 |
| **Container** | **Docker & Docker Compose** | v29.9 / v5.7 |

---
