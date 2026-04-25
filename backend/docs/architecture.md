# Architecture

## 개요

이 백엔드는 “방(Room) 기반 채팅”을 제공하는 Spring Boot 애플리케이션입니다.

- **Room/User 등 핵심 도메인 데이터**: JPA(MySQL)
- **채팅 메시지 히스토리**: MongoDB
- **실시간 메시지 브로커링/팬아웃, 토큰 블랙리스트/캐시**: Redis
- **실시간 통신**: WebSocket(STOMP, SockJS)
- **인증**: JWT 기반(stateless), HTTP + STOMP 모두 동일한 Access Token 정책 사용

## 모듈(패키지) 관계

패키지 루트는 `com.funchat.demo` 입니다.

- `user`
  - HTTP 인증 관련 엔드포인트(`/api/auth/**`) 제공
  - `auth` 모듈(JWT/유저디테일)과 강하게 연동
- `room`
  - 방 생성/조회/수정/삭제, 입장/퇴장, 매니저 위임
  - JPA 엔티티 `Room`, `User.room` 관계를 통해 참여 상태를 관리
- `chat`
  - WebSocket(STOMP) 인바운드 인증/권한 검증(구독/전송 시 방 참여자 여부 확인)
  - Redis Streams / PubSub 기반 브로커를 통해 메시지 fanout 및 Mongo 저장 경로 제공
  - HTTP로 채팅 히스토리 조회 API 제공
- `global`
  - Security/WebSocket/CORS/Redis/Mongo 등 공통 인프라 설정
  - 필터/예외/상수/로깅(AOP) 등 횡단 관심사
- `util`
  - 응답 포맷(`ResponseUtil`), 파싱 유틸 등

## 요청 흐름

### HTTP API 흐름

1. `SecurityFilterChain`에서 JWT 인증 필터가 `Authorization` 헤더를 처리
2. Controller는 DTO로 입력을 받고, Service를 호출
3. 성공 응답은 `ResponseUtil.createSuccessResponse(...)`로 래핑되어 `ResponseDto` 형태로 반환

응답 공통 포맷:

- `ResponseDto(code, message, body)`

### WebSocket(STOMP) 흐름

1. 클라이언트는 `/ws` 엔드포인트로 SockJS 연결
2. STOMP 인바운드 채널 인터셉터(`StompHandler`)가 `CONNECT`에서 토큰 인증
3. `SUBSCRIBE` 시:
   - `/sub/chat/{roomId}` 구독이면 roomId 파싱
   - “해당 roomId에 사용자가 참여자인지” 검증
4. `SEND` 시:
   - native header에서 `roomId`를 읽고 참여자 검증

브로커 라우팅:

- 구독 prefix: `/sub`
- 발행 prefix: `/pub`

### 메시지 저장/팬아웃(브로커) 흐름(개념)

`MessageBrokerChatService`에서 두 경로를 사용합니다.

- **Durable path**: Redis(Streams 등) → 구독 → `ChatService.saveMessageToMongo(...)` → Mongo 저장
- **Fanout path**: Redis(PubSub 등) → 구독 → `SimpMessagingTemplate.convertAndSend("/sub/chat/{roomId}", dto)` → 각 인스턴스가 로컬 WS 세션으로 전송

## 확장/변경 시 가이드

- **응답 포맷**은 기존 컨벤션을 유지(프로토콜 호환성)
- **인증/권한** 로직은 HTTP는 필터, STOMP는 `StompHandler` 경로에 집중
- **채팅 저장소 변경**(Mongo 스키마/인덱스 등)은 `chat` 도메인 + `docs/db.md`를 함께 업데이트

