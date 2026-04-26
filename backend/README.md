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

## 주요 기능

- 이메일/비밀번호 기반 회원가입 및 로그인
- JWT access/refresh token 발급, 재발급, 로그아웃
- 채팅방 생성, 목록 조회, 상세 조회, 수정, 삭제
- 채팅방 입장, 퇴장, 매니저 위임
- STOMP 기반 실시간 메시지 송수신
- Redis Streams/PubSub 기반 메시지 저장 경로와 팬아웃 경로 분리
- MongoDB 기반 채팅 메시지 이력 조회
- Actuator/Prometheus 메트릭 노출

## 아키텍처

이 백엔드는 방(Room) 기반 채팅을 제공하는 Spring Boot 애플리케이션입니다.

- Room/User 등 핵심 도메인 데이터는 MySQL/JPA에 저장합니다.
- 채팅 메시지 히스토리는 MongoDB에 저장합니다.
- 실시간 메시지 브로커링, 팬아웃, 토큰 블랙리스트, 캐시는 Redis를 사용합니다.
- 실시간 통신은 WebSocket(STOMP, SockJS)을 사용합니다.
- HTTP와 STOMP 모두 JWT access token 기반 stateless 인증 정책을 사용합니다.

### 패키지 구조

```text
com.funchat.demo
├── auth      # JWT, UserDetails, 인증 유틸
├── user      # 회원가입, 로그인, 토큰 재발급, 로그아웃
├── room      # 채팅방 CRUD, 입장/퇴장, 매니저 위임
├── chat      # STOMP 처리, 메시지 브로커링, 채팅 이력 조회
├── global    # 설정, 필터, 예외, 공통 DTO, AOP, 상수
└── util      # ResponseUtil, ParseUtil 등 공통 유틸
```

권장 레이어링은 다음을 기준으로 합니다.

- `**/controller`: HTTP API 엔드포인트
- `**/service`: 비즈니스 로직, 트랜잭션, 도메인 조합
- `**/domain`: 엔티티, 리포지토리, 도메인 모델
- `**/domain/dto`: 요청/응답 DTO
- `global`: config/filter/exception/constants/aop/annotation 등 공통 인프라
- `util`: 범용 유틸

### 요청 흐름

HTTP API는 `SecurityFilterChain`의 JWT 인증 필터가 `Authorization` 헤더를 처리한 뒤 Controller, Service, Domain 순서로 실행됩니다. 성공 응답은 `ResponseUtil.createSuccessResponse(...)`로 래핑되어 `ResponseDto(code, message, body)` 형식으로 반환합니다.

WebSocket은 클라이언트가 `/ws`로 SockJS 연결한 뒤 STOMP native header `Authorization`에 access token을 전달합니다. `StompHandler`는 `CONNECT`에서 토큰을 인증하고, `SUBSCRIBE`와 `SEND`에서 사용자가 해당 채팅방 참여자인지 검증합니다.

메시지 브로커는 저장 경로와 팬아웃 경로를 분리합니다.

- Durable path: Redis Streams 계열 경로에서 메시지를 구독해 MongoDB에 저장
- Fanout path: Redis Pub/Sub 계열 경로에서 `/sub/chat/{roomId}`로 메시지 전파

## HTTP API 요약

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

상세 API 계약은 [SPEC.md](SPEC.md)를 기준으로 합니다.

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
- MongoDB: 채팅 메시지 영속 저장 및 Mongo `_id` 기반 커서 조회
- Redis: 메시지 브로커링, Pub/Sub 팬아웃, Streams 기반 저장 경로, 로그아웃 토큰 블랙리스트, 캐시

## 개발 규칙

### 계층 규칙

- Controller는 입력 검증(`@Valid`)과 인증 주입(`@AuthenticationPrincipal`)까지만 담당하고 비즈니스/영속 로직을 직접 수행하지 않습니다.
- Service는 비즈니스 규칙의 중심이며 트랜잭션 경계를 관리합니다.
- Domain은 도메인 상태와 행위를 표현하고, 외부 리소스 접근은 Service에 둡니다.
- Repository는 `domain` 하위에 위치합니다.

### 기본 스타일

- 인코딩은 UTF-8, 줄바꿈은 LF, 들여쓰기는 4 spaces를 사용합니다.
- 파일명은 PascalCase + 용도(`UserController`, `RoomService`)로 작성합니다.
- 와일드카드 import와 FQCN 직접 사용을 피합니다.
- 가능한 곳에는 `final`을 사용합니다.
- DTO는 record를 우선 사용하고 Request/Response를 분리합니다.
- 반복 문자열과 매직 넘버는 상수로 추출하되, 에러 메시지는 상수화하지 않습니다.

### Lombok 및 네이밍

- 엔티티는 `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 기본으로 합니다.
- 서비스/리포지토리/컴포넌트는 `@RequiredArgsConstructor`로 생성자 주입을 통일합니다.
- 클래스/인터페이스는 `PascalCase`, 메서드/변수는 `camelCase`, 상수는 `UPPER_SNAKE_CASE`를 사용합니다.
- DTO 이름은 `...Request`, `...Response`로 끝냅니다.
- 조회 메서드는 `find`/`get`, 쓰기 메서드는 `create`/`update`/`delete`를 사용합니다.
- Boolean 판별 메서드는 `is`/`has`/`can` 접두어를 사용합니다.

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

기능 구현 시 테스트 작성 및 통과를 완료 기준으로 봅니다. 테스트 전략은 [testing.md](testing.md)를 따릅니다.

## 개발 문서

- [AGENTS.md](AGENTS.md): Codex/agent 작업 규칙
- [SPEC.md](SPEC.md): 기능 명세와 API 계약
- [PLAN.md](PLAN.md): 진행 상황과 작업 계획
- [testing.md](testing.md): 테스트 작성 및 실행 기준
