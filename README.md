# FunChat

> JWT 기반 인증과 STOMP(WebSocket) 실시간 채팅, Redis Streams·MongoDB를 활용한 메시지 처리로 다중 사용자 채팅방을 제공하는 서비스

---

## 배포·운영

| 구분            | 설명                                                                                                |
| --------------- | --------------------------------------------------------------------------------------------------- |
| 컨테이너 이미지 | Docker Hub: `changbill/funchat-backend`, `changbill/funchat-frontend`                               |
| CI/CD           | [Jenkinsfile](./Jenkinsfile) — 백엔드·프론트엔드 Docker 빌드 → 푸시 → 미니PC에서 `docker compose` 배포 |
| 배포 스택       | [`deploy/`](./deploy/) — Rolling slots + Router(Nginx) + Infra(MySQL/Mongo/Redis)                   |
| 서비스 URL      | URL: https://funchat.changee.cloud/                                                                 |

배포 healthcheck는 백엔드 `/health`를 사용한다. 이 엔드포인트는 MySQL, Redis, MongoDB 연결까지 확인하며, 롤링 슬롯 교체 후 smoke test에도 사용된다.

---

## 프로젝트 구조

```
funchat/
├── backend/                 # Spring Boot 백엔드 API·WebSocket 서버
├── frontend/funchat/        # 사용자 웹 (React + Vite)
├── monitoring/              # Prometheus 설정·부하 테스트 스크립트 (k6 등)
├── deploy/                  # 배포용 compose/nginx(rolling slots, router, infra)
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

- **Redis Streams + Pub/Sub**: Streams로 메시지를 적재해 **내구성 있게 저장(MongoDB)** 하고, Pub/Sub로 **모든 백엔드 인스턴스가 수신**해 각 인스턴스의 `SimpMessagingTemplate`으로 방 단위(`/sub/chat/{roomId}`) 실시간 푸시
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

| 항목                         | 설명                                                                    |
| ---------------------------- | ----------------------------------------------------------------------- |
| [`deploy/`](./deploy/)       | Rolling 슬롯(`app-1..3`, `web-1..3`) + surge 슬롯(`app-4`, `web-4`) + Router(Nginx) + Infra(MySQL/Mongo/Redis) |
| [monitoring/](./monitoring/) | Prometheus 설정, 부하 테스트용 스크립트 등                              |

### Jenkins 배포 파일 동기화

Jenkins는 [`deploy/scripts/jenkins-remote-deploy.sh`](./deploy/scripts/jenkins-remote-deploy.sh)를 호출한다. 이 스크립트는 원격 미니PC의 `~/funchat/deploy.next`에 `deploy/` 내부 파일을 먼저 전송한 뒤, 성공하면 `~/funchat/deploy`로 교체한다. 원격에 `deploy/deploy` 중첩 폴더나 삭제된 오래된 배포 파일이 남는 것을 막기 위한 방식이다.

LiveKit은 `deploy/docker-compose.livekit.yml`로 별도 실행한다. Jenkins 앱 배포는 LiveKit 컨테이너를 자동으로 기동하지 않는다.

### 배포 스크립트 구조

| 파일 | 역할 |
| --- | --- |
| [`deploy/scripts/docker-push-images.sh`](./deploy/scripts/docker-push-images.sh) | Jenkins에서 Docker Hub 로그인, 이미지 push, logout 처리 |
| [`deploy/scripts/jenkins-remote-deploy.sh`](./deploy/scripts/jenkins-remote-deploy.sh) | Jenkins에서 원격 deploy 폴더 동기화, 임시 secret 전송, 원격 배포 실행 |
| [`deploy/scripts/deploy-common.sh`](./deploy/scripts/deploy-common.sh) | 배포 스크립트 공통 credential 로딩, cleanup, Docker login/logout, HTTP 요청 함수 |
| [`deploy/deploy.sh`](./deploy/deploy.sh) | 미니PC에서 실행되는 롤링 배포 본체 |

### 운영 배포 방식

운영 배포는 미니PC 단일 호스트에 맞춘 롤링 방식이다.

- `deploy/docker-compose.rolling.yml`은 상시 슬롯 3개(`app-1..3`, `web-1..3`)와 배포 중 임시 surge 슬롯 1개(`app-4`, `web-4`)를 고정 컨테이너 이름으로 정의한다.
- `APP_REPLICAS`는 상시 사용할 슬롯 수를 의미하며 기본값은 `3`이다. 현재 상시 슬롯은 최대 3개까지 사용한다.
- `deploy/deploy.sh`는 backend 배포 시작 시 surge backend 슬롯을 한 번 생성하고, 모든 backend 슬롯 교체 동안 upstream에 유지한다.
- frontend도 같은 방식으로 surge frontend 슬롯을 한 번 생성하고, 모든 frontend 슬롯 교체 동안 upstream에 유지한다.
- surge 슬롯이 유지된 상태에서 기존 슬롯을 하나씩 upstream에서 제외하고 재생성하므로, 배포 중에도 라우팅 대상 수가 `APP_REPLICAS` 아래로 내려가지 않는다.
- Nginx upstream 블록과 공통 설정은 `deploy/nginx/upstream.conf`에 두고, 배포 중 바뀌는 server 목록만 `upstream.web.servers.conf`, `upstream.app.servers.conf`에 갱신한다.
- 새 백엔드 슬롯은 `/health`가 healthy가 된 뒤 upstream에 투입된다.
- 각 슬롯 교체 후 Nginx 경유 `/health`와 `/` smoke test를 실행한다.
- backend/frontend 각 배포 구간이 끝나면 해당 surge 슬롯은 upstream에서 제외되고 중지된다.
- 배포 중 실패하면 가용성을 우선해 진행 중이던 문제 슬롯은 제외하고 surge 슬롯은 upstream에 남긴다.

### LiveKit 단독 테스트

LiveKit은 앱 롤링 배포와 분리된 단독 compose로 먼저 검증한다.

```bash
cd deploy
docker compose -f docker-compose.livekit.yml up -d
```

- 설정 파일: [`deploy/livekit.yml`](./deploy/livekit.yml)
- 기본 signaling/API 포트: `7880/tcp`
- WebRTC media 포트: `50000-50100/udp`
- ICE TCP fallback: `7881/tcp`
- 현재 `devkey/devsecret-change-me-before-use-000000`은 임시 테스트용이므로 외부 공개 전 반드시 교체한다.

---

## 로컬 실행 요약

1. `backend`에서 Spring Boot를, `frontend/funchat`에서 `npm run dev`로 각각 기동한다.
2. 백엔드가 기대하는 MySQL·MongoDB·Redis는 로컬 또는 컨테이너로 띄운다.

### 로컬(도커 컴포즈)로 한 번에 실행

- **실행**: `deploy/docker-compose.local.yml` 사용
- **노출 포트**: `80`(router), `8080`(backend), `3306`(mysql), `27017`(mongo), `6379`(redis)
