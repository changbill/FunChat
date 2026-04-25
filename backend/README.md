# FunChat Backend

Spring Boot 기반 FunChat 백엔드입니다. JWT 인증, 채팅방 관리, STOMP(WebSocket) 실시간 메시징, Redis 기반 브로커링, MongoDB 채팅 이력 저장을 담당합니다.

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4, Spring MVC, Spring Security, WebSocket(STOMP/SockJS) |
| Build | Gradle |
| RDB | MySQL + JPA |
| Document DB | MongoDB |
| Cache/Broker | Redis, Redis Streams, Redis Pub/Sub |
| Test | JUnit 5, Spring Boot Test, Testcontainers, H2 |
| Observability | Spring Boot Actuator, Prometheus registry |

## 패키지 구조

```text
com.funchat.demo
├── auth      # JWT, UserDetails, 인증 유틸
├── user      # 회원가입, 로그인, 토큰 재발급, 로그아웃
├── room      # 채팅방 CRUD, 입장/퇴장, 매니저 위임
├── chat      # STOMP 처리, 메시지 브로커링, 채팅 이력 조회
├── global    # 설정, 필터, 예외, 공통 DTO, AOP, 상수
└── util      # ResponseUtil, ParseUtil 등 공통 유틸
```

자세한 설계는 [docs/architecture.md](docs/architecture.md)를 기준으로 합니다.

## 주요 기능

- 이메일/비밀번호 기반 회원가입 및 로그인
- JWT access/refresh token 발급, 재발급, 로그아웃
- 채팅방 생성, 목록 조회, 상세 조회, 수정, 삭제
- 채팅방 입장, 퇴장, 매니저 위임
- STOMP 기반 실시간 메시지 송수신
- Redis Streams/PubSub 기반 메시지 저장 경로와 팬아웃 경로 분리
- MongoDB 기반 채팅 메시지 이력 조회
- Actuator/Prometheus 메트릭 노출

## HTTP API 요약

모든 HTTP 성공 응답은 `ResponseUtil.createSuccessResponse(...)`를 통해 `ResponseDto(code, message, body)` 형식으로 반환합니다.

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인 및 토큰 발급 |
| POST | `/api/auth/reissue` | Refresh token | 토큰 재발급 |
| POST | `/api/auth/logout` | Access token | 로그아웃 및 access token 블랙리스트 처리 |
| POST | `/api/rooms` | 필요 | 채팅방 생성 |
| GET | `/api/rooms` | 필요 | 채팅방 목록 조회 |
| GET | `/api/rooms/{roomId}` | 필요 | 채팅방 상세 조회 |
| PATCH | `/api/rooms/{roomId}` | 필요 | 채팅방 수정 |
| DELETE | `/api/rooms/{roomId}` | 필요 | 채팅방 삭제 |
| POST | `/api/rooms/{roomId}/enter` | 필요 | 채팅방 입장 |
| POST | `/api/rooms/{roomId}/leave` | 필요 | 채팅방 퇴장 |
| PATCH | `/api/rooms/{roomId}/manager?newManagerId={userId}` | 필요 | 매니저 위임 |
| GET | `/api/chat/messages/{roomId}` | 필요 | 커서 기반 채팅 이력 조회 |
| GET | `/actuator/health` | 불필요 | 헬스 체크 |
| GET | `/actuator/prometheus` | 불필요 | Prometheus 메트릭 |

API 작성 규칙은 [docs/api-patterns.md](docs/api-patterns.md)를 기준으로 합니다.

## WebSocket/STOMP

| 항목 | 값 |
| --- | --- |
| SockJS endpoint | `/ws` |
| Publish prefix | `/pub` |
| Subscribe prefix | `/sub` |
| 메시지 발행 destination | `/pub/chat/message` |
| 채팅방 구독 destination | `/sub/chat/{roomId}` |

STOMP 연결 시 native header `Authorization`에 access token을 전달합니다. 구독 및 메시지 전송 시에는 사용자가 해당 채팅방 참여자인지 검증합니다.

## 저장소 역할

- MySQL/JPA: 사용자, 채팅방 등 코어 도메인 데이터
- MongoDB: 채팅 메시지 영속 저장 및 커서 기반 이력 조회
- Redis: 메시지 브로커링, Pub/Sub 팬아웃, Streams 기반 저장 경로, 로그아웃 토큰 블랙리스트, 캐시

상세 규칙은 [docs/db.md](docs/db.md)를 참고합니다.

## 로컬 실행

필수 환경변수는 `.env` 또는 실행 환경에서 주입합니다.

| 변수 | 설명 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `USER_ID` | MySQL/Mongo 사용자명 |
| `USER_PW` | MySQL/Mongo 비밀번호 |
| `ROOT_PW` | 로컬 MySQL root 비밀번호 |
| `SPRING_DATA_REDIS_HOST` | Redis host |
| `SPRING_DATA_MONGODB_HOST` | MongoDB host |
| `JWT_SECRET` | JWT 서명 secret |
| `ACCESS_EXPIRATION` | Access token 만료 시간 |
| `REFRESH_EXPIRATION` | Refresh token 만료 시간 |
| `CORS_ALLOWED_ORIGINS` | 허용 origin 목록, comma-separated |

인프라 컨테이너 실행:

```powershell
cd backend
docker compose up -d
```

애플리케이션 실행:

```powershell
cd backend
.\gradlew.bat bootRun
```

## 테스트

```powershell
cd backend
.\gradlew.bat test
```

기능 구현 시 테스트 작성 및 통과를 완료 기준으로 봅니다. 테스트 전략은 [docs/testing.md](docs/testing.md)를 따릅니다.

## 개발 문서

- [AGENTS.md](AGENTS.md): Codex/agent 작업 규칙
- [docs/SPEC.md](docs/SPEC.md): 기능 명세와 API 계약
- [docs/PLAN.md](docs/PLAN.md): 진행 상황과 작업 계획
- [docs/architecture.md](docs/architecture.md): 아키텍처와 요청 흐름
- [docs/conventions.md](docs/conventions.md): 코드 스타일과 계층 규칙
- [docs/api-patterns.md](docs/api-patterns.md): API 응답/컨트롤러/STOMP 패턴
- [docs/db.md](docs/db.md): 저장소별 역할과 규칙
- [docs/security.md](docs/security.md): 인증/보안 정책
- [docs/testing.md](docs/testing.md): 테스트 작성 및 실행 기준
