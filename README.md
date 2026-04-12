# FunChat

> JWT 기반 인증과 STOMP(WebSocket) 실시간 채팅, Redis Streams·MongoDB를 활용한 메시지 처리로 다중 사용자 채팅방을 제공하는 서비스

---

## 배포·운영

| 구분            | 설명                                                                                                |
| --------------- | --------------------------------------------------------------------------------------------------- |
| 컨테이너 이미지 | Docker Hub: `changbill/funchat-backend`, `changbill/funchat-frontend`                               |
| CI/CD           | [Jenkinsfile](./Jenkinsfile) — 백엔드·프론트엔드 Docker 빌드 → 푸시 → EC2에서 `docker compose` 배포 |
| 로컬 스택       | [docker-compose.yml](./docker-compose.yml) — Nginx 프론트, Spring Boot, MySQL, MongoDB, Redis       |
| 서비스 URL      | URL: https://funchat.changee.cloud/                                                                 |

---

## 프로젝트 구조

```
funchat/
├── backend/                 # Spring Boot 백엔드 API·WebSocket 서버
├── frontend/funchat/        # 사용자 웹 (React + Vite)
├── monitoring/              # Prometheus 설정·부하 테스트 스크립트 (k6 등)
├── docker-compose.yml       # 로컬/서버 컨테이너 구성 (MySQL·MongoDB·Redis 포함)
└── Jenkinsfile              # Docker 빌드·배포 파이프라인
```

---

## 1. Backend

**경로**: [`backend/`](./backend/)

### 아키텍처

기능 단위로 패키지가 나뉘어 있으며, 도메인·서비스·설정·예외 처리를 분리한다.

```
com.funchat.demo/
├── user/           # 회원, 인증 API
├── room/           # 채팅방 CRUD, 입장·퇴장, 매니저 위임 등
├── chat/           # STOMP 메시지, 채팅 이력, Redis Streams 연동
├── auth/           # JWT, Security 연동 (UserDetails 등)
└── global/         # 공통 설정(Security, WebSocket, CORS, Redis), 예외, AOP 로깅
```

### 주요 API

| 분류                                          | 기능                                                               |
| --------------------------------------------- | ------------------------------------------------------------------ |
| **인증** (`/api/auth`)                        | 회원가입, 로그인, 토큰 재발급, 로그아웃                            |
| **채팅방** (`/api/rooms`)                     | 생성·목록·상세·수정·삭제, 입장·퇴장, 매니저 위임 (인증 필요)       |
| **채팅 이력** (`/api/chat/messages/{roomId}`) | 커서 기반 페이지 조회 (MongoDB)                                    |
| **실시간**                                    | STOMP 엔드포인트 `/ws` (SockJS), `/pub/chat/message`로 메시지 발행 |

### 메시지·저장소 역할

- **Redis Streams**: 채팅 메시지를 스트림에 적재하고 구독자가 소비한 뒤, `SimpMessagingTemplate`으로 방 단위(`/sub/room/{roomId}`) 브로드캐스트
- **MongoDB**: 채팅 메시지 영속 저장, 이력 조회(커서·역순)
- **MySQL (JPA)**: 사용자, 채팅방 등 관계형 데이터

### 기타

- **Spring Security + JWT**: REST 및 STOMP 인증
- **Actuator + Prometheus**: 메트릭 노출 ([`monitoring/prometheus.yml`](./monitoring/prometheus.yml)에서 스크랩 예시)
- **테스트**: JUnit, Testcontainers 등 ([`backend/src/test`](./backend/src/test))

---

## 2. Frontend (User Web)

**경로**: [`frontend/funchat/`](./frontend/funchat/)

### 스택

- React 19, React Router, Vite
- SockJS + STOMP로 WebSocket 연결

### 주요 화면

| 화면                         | 기능                                           |
| ---------------------------- | ---------------------------------------------- |
| **로그인 / 회원가입**        | 계정 기반 로그인·가입                          |
| **채팅방 목록** (`/`)        | 방 목록, 인증된 사용자만 접근 (`PrivateRoute`) |
| **채팅방** (`/room/:roomId`) | 실시간 메시지 송수신, 채팅 UI                  |

프로덕션에서는 Nginx로 정적 파일을 서빙하도록 [Dockerfile](./frontend/funchat/Dockerfile)·[`nginx.conf`](./frontend/funchat/nginx.conf) 구성을 사용한다.

---

## 3. 인프라·모니터링

| 항목                                       | 설명                                                              |
| ------------------------------------------ | ----------------------------------------------------------------- |
| [docker-compose.yml](./docker-compose.yml) | `web`(Nginx+프론트), `app`(백엔드), MySQL, MongoDB, Redis 및 볼륨 |
| [monitoring/](./monitoring/)               | Prometheus 설정, 부하 테스트용 스크립트 등                        |

---

## 로컬 실행 요약

1. 루트의 `docker-compose.yml`에 맞춰 환경 변수( DB·JWT·CORS 등)를 설정한다.
2. 또는 `backend`에서 Spring Boot를, `frontend/funchat`에서 `npm run dev`로 각각 기동하고, 백엔드가 기대하는 MySQL·MongoDB·Redis를 로컬 또는 컨테이너로 띄운다.
