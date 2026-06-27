# Backend AGENTS.md

## 1. 백엔드 프로젝트 요약

Funchat 백엔드는 API, 인증, 채팅방, 실시간 메시징, 메시지 영속화, 영상 세션 관리를 담당한다.

- 언어: Java 21
- 프레임워크: Spring Boot
- 빌드 도구: Gradle
- 기본 패키지 루트: `com.funchat.demo`

주요 기술:

- Spring MVC
- Spring Security
- JWT
- WebSocket
- STOMP
- SockJS
- Redis
- MongoDB
- JPA
- MySQL
- LiveKit (SFU, WebRTC signaling/media)

---

## 2. 패키지 구조

백엔드 코드는 다음 패키지 구분을 따른다.

```text
com.funchat.demo
├── auth
├── user
├── room
├── chat
├── video
├── global
└── util
```

| 패키지   | 역할                                                                                   |
| -------- | -------------------------------------------------------------------------------------- |
| `auth`   | JWT, `UserDetails`, 인증 필터, 인증/인가 처리                                          |
| `user`   | 회원가입, 로그인, 토큰 재발급, 로그아웃 API                                            |
| `room`   | 채팅방 생성, 조회, 입장, 퇴장, 매니저 위임                                             |
| `chat`   | WebSocket STOMP 인바운드 처리, Redis 브로커, MongoDB 메시지 저장, HTTP 메시지 조회 API |
| `video`  | 방 단위 영상 세션 생성/조회, LiveKit 참가 토큰 발급, 세션 상태 관리                    |
| `global` | 공통 설정, 필터, 예외, 상수, AOP                                                       |
| `util`   | 공통 응답 포맷, 파싱, 공통 유틸                                                        |

구조 규칙:

- 특정 도메인에만 필요한 코드는 해당 도메인 패키지에 둔다.
- 여러 도메인에서 재사용되는 코드만 `global` 또는 `util`로 분리한다.
- 공통화가 필요하면 근거를 정리한 뒤 `global` 또는 `util`로 분리한다.
- 새 패키지를 만들기 전 기존 패키지 역할과 중복되는지 확인한다.
- 기존 패키지 구조와 `Controller → Service → Domain` 흐름을 유지한 채 변경 범위를 최소화한다.

---

## 3. 계층 구조

백엔드는 다음 흐름을 유지한다.

```text
Controller → Service → Domain(Entity/Repository)
```

### Controller

- HTTP 요청과 응답을 처리한다.
- 요청 값, PathVariable, RequestBody, 인증 사용자 정보를 Service에 전달한다.
- 비즈니스 로직, 토큰 파싱, 권한 판단, DB 조회는 Service에 위임한다.
- 응답은 `ResponseUtil`을 사용해 `ResponseDto(code, message, body)` 형식으로 반환한다.

### Service

- 비즈니스 흐름을 담당한다.
- 트랜잭션 경계를 관리한다.
- Entity 조회, 검증, 상태 변경을 처리한다.
- Controller 전용 로직을 포함하지 않는다.
- 여러 저장소를 함께 사용하는 경우 정합성 기준을 명확히 한다.

### Repository

- 데이터 접근을 담당한다.
- 비즈니스 판단 로직을 포함하지 않는다.
- 단순 조회는 Spring Data JPA 메서드명을 우선한다.
- 복잡한 조회는 기존 프로젝트 방식에 맞춰 `@Query` 등을 사용한다.
- 조회 조건, 정렬, 페이징이 있는 경우 테스트에서 검증한다.

### Entity

- DB 테이블과 매핑되는 상태를 가진다.
- 상태 변경은 의미 있는 메서드로 표현한다.
- 무분별한 Setter 추가를 피한다.
- 연관관계 변경 시 기존 매핑과 영속성 흐름을 확인한다.
- API 응답은 DTO로 변환해 반환한다.

---

## 4. 응답 포맷

컨트롤러는 기존 응답 포맷을 유지한다.

```java
ResponseDto(code, message, body)
```

반환 시 `ResponseUtil`을 사용한다.

규칙:

- 새 API도 `ResponseUtil`과 `ResponseDto(code, message, body)` 형식을 따른다.
- Entity는 DTO로 변환한 뒤 응답 body에 담는다.
- 에러 응답은 `global.exception` 흐름을 따른다.
- 클라이언트 응답에는 내부 예외 메시지, 스택트레이스, SQL 정보를 포함하지 않는다.

---

## 5. 인증 규칙

### HTTP 인증

HTTP 요청은 다음 형식을 사용한다.

```http
Authorization: Bearer <access-token>
```

규칙:

- HTTP 토큰 검증은 필터에서 처리한다.
- Controller는 `@AuthenticationPrincipal` 등 기존 방식으로 인증 사용자 정보를 받아 Service에 전달한다.
- 인증 실패와 권한 없음은 각각 구분해 응답한다.

### WebSocket 인증

WebSocket STOMP 요청은 native header `Authorization`으로 토큰을 전달한다.

규칙:

- WebSocket 토큰 검증은 인바운드 채널 인터셉터에서 처리한다.
- HTTP 인증 흐름과 WebSocket 인증 흐름을 각각 유지한다.

---

## 6. 저장소 사용 규칙

| 용도                                  | 저장소     |
| ------------------------------------- | ---------- |
| User, Room 등 코어 데이터             | JPA, MySQL |
| 채팅 메시지 영속                      | MongoDB    |
| 실시간 처리, 팬아웃, 블랙리스트, 캐시 | Redis      |
| WebRTC signaling/media                | LiveKit    |

규칙:

- User, Room 등 관계형 데이터는 JPA와 MySQL 흐름을 유지한다.
- 채팅 메시지는 MongoDB 저장 흐름을 유지한다.
- Redis는 실시간 처리, 팬아웃, 블랙리스트, 캐시에 사용한다.
- 영상 미디어 전송과 signaling은 LiveKit이 담당한다. 백엔드는 세션 상태와 참가 토큰만 관리한다.
- FunChat JWT는 API 인증에, LiveKit JWT는 영상 참가에 사용한다.
- 저장소 변경이 필요하면 변경 근거를 먼저 정리한다.

---

## 7. 채팅 처리 규칙

- WebSocket 인바운드 처리는 `chat` 패키지의 기존 흐름을 따른다.
- STOMP 메시지 처리, Redis 브로커, MongoDB 저장 책임을 분리한다.
- 실시간 팬아웃은 Redis, 메시지 영속은 MongoDB가 담당한다.
- Redis Pub/Sub은 fanout, Redis Streams는 저장 경로로 각각 사용한다.
- Redis Pub/Sub 또는 Redis Streams 사용처를 변경할 때는 메시지 유실, 중복 처리, 재처리 가능성을 검토한다.
- 메시지 저장 실패, Redis 처리 실패, WebSocket 전송 실패 시나리오를 고려한다.

---

## 8. 트랜잭션 규칙

- 트랜잭션은 Service 계층에서 관리한다.
- 조회 메서드는 가능하면 `@Transactional(readOnly = true)`를 사용한다.
- 생성, 수정, 삭제 메서드는 `@Transactional`을 사용한다.
- Controller와 Repository에는 트랜잭션을 두지 않는다.
- DB 변경과 외부 시스템 호출이 함께 있으면 실패 시나리오를 고려한다.
- Redis, MongoDB, MySQL을 함께 다루는 작업은 정합성 기준을 명확히 한다.

---

## 9. 예외 처리 규칙

- 예외 처리는 기존 `global.exception` 흐름을 따른다.
- Controller에서 try-catch로 응답을 직접 만들지 않는다.
- 도메인별 예외는 의미가 드러나게 정의한다.
- 에러 응답 포맷은 기존 공통 응답 규칙을 유지한다.
- 인증 실패, 권한 없음, 리소스 없음, 검증 실패를 구분한다.
- 내부 구현 세부사항을 클라이언트 응답에 노출하지 않는다.

---

## 10. 테스트 컨벤션

기능 구현 또는 수정 시 테스트를 작성한다.

테스트 기준:

- 핵심 비즈니스 로직을 변경할 때는 테스트를 함께 추가하거나 갱신하고 `./gradlew test`로 통과를 확인한다.
- Service 테스트는 비즈니스 규칙을 검증한다.
- Controller 테스트는 요청, 응답, 인증, 검증 실패를 확인한다.
- Repository 테스트는 쿼리 조건, 정렬, 페이징을 확인한다.
- Redis, MongoDB, MySQL 연동이 필요한 경우 `testing.md`의 Testcontainers 전략을 따른다.
- 외부 연동은 테스트 대역을 우선 사용한다.

테스트명은 검증하려는 동작이 드러나게 작성한다.

예시:

```java
@Test
void 방장이_퇴장하면_다음_참여자에게_매니저가_위임된다() {
}
```

---

## 11. 백엔드 검증 명령

백엔드 작업 완료 후 가능한 검증을 수행한다.

```bash
./gradlew test
./gradlew build
```
