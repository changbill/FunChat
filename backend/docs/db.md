# Database / Storage

## 개요

이 프로젝트는 여러 저장소를 역할별로 분리해 사용합니다.

- **MySQL + JPA**: Room/User 등 “코어 도메인” 영속
- **MongoDB**: 채팅 메시지 히스토리 저장(커서 기반 조회)
- **Redis**: 실시간 메시지 브로커(Streams/PubSub), 토큰 블랙리스트/캐시
- **H2(in-memory)**: 테스트용(JPA)

## JPA (MySQL)

- 엔티티는 `room/domain`, `user/domain` 등에 위치합니다.
- 관계 예시:
  - `User` → `Room` : `@ManyToOne` (`User.room`)
  - `Room` → `User` participants: `@OneToMany(mappedBy="room")`

로컬/배포에서 실제 MySQL 설정은 환경변수/설정 파일에 의해 주입됩니다(프로젝트 구성에 맞게 확인).

## MongoDB (Chat Messages)

- 채팅 메시지 저장은 `ChatService.saveMessageToMongo(...)` 경로를 따릅니다.
- 메시지 조회는 `_id` 내림차순 정렬 + `idLessThan(cursorId)` 형태의 커서 기반 슬라이스 조회를 사용합니다.

운영에서 성능/조회 패턴을 변경한다면(예: 인덱스, 정렬 필드), 조회 로직과 DTO를 함께 변경합니다.

## Redis

### 용도

- **실시간 메시지 브로커링**
  - Durable path(저장): Redis → 구독 → Mongo 저장
  - Fanout path(전파): Redis → 구독 → WebSocket 세션으로 전송
- **보안**
  - Access token 블랙리스트(로그아웃 토큰 무효화)
- **캐시**
  - `@EnableCaching` 기반 캐시(기본 TTL 10분 설정)

### 캐시 설정 주의

- `RedisCacheConfig`에서 CacheManager의 default TTL이 10분으로 설정되어 있습니다.
- 직렬화는 key는 문자열, value는 JSON 기반 직렬화를 사용합니다.

## 테스트 저장소 구성

테스트는 다음 조합을 사용합니다.

- JPA: H2 in-memory
- Redis/Mongo: Testcontainers로 컨테이너 실행

자세한 실행/전략은 `docs/testing.md`를 참고하세요.

