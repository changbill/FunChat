# FunChat Backend Specification

이 문서는 FunChat 백엔드의 현재 기능 계약을 정의합니다. 구현 변경 시 API, 데이터 흐름, 인증 정책이 바뀌면 이 문서를 함께 갱신합니다.

## 서비스 범위

FunChat 백엔드는 다음 책임을 가집니다.

- 사용자 계정 생성, 로그인, 토큰 재발급, 로그아웃
- JWT 기반 HTTP API 인증
- STOMP native header 기반 WebSocket 인증
- 채팅방 생성, 조회, 수정, 삭제
- 채팅방 입장/퇴장 및 매니저 위임
- 채팅 메시지 실시간 전송
- 채팅 메시지 영속 저장 및 이력 조회
- 운영 헬스 체크와 Prometheus 메트릭 노출

## 공통 HTTP 계약

### 응답 형식

모든 HTTP Controller 성공 응답은 다음 형식을 유지합니다.

```json
{
  "code": 200,
  "message": "성공",
  "body": {}
}
```

- `code`: HTTP status code 값
- `message`: 응답 메시지
- `body`: 실제 응답 데이터, 없으면 `null`

Controller는 `ResponseUtil.createSuccessResponse(body)`를 사용합니다.

### 인증 헤더

| 목적 | Header | 형식 |
| --- | --- | --- |
| Access token | `Authorization` | `Bearer <access-token>` |
| Refresh token | `Authorization-Refresh` | `Bearer <refresh-token>` |

`/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout`, `/ws`, `/actuator/**`를 제외한 HTTP API는 인증을 요구합니다.

## Auth API

### 회원가입

`POST /api/auth/signup`

Request:

```json
{
  "email": "user@example.com",
  "password": "password",
  "nickname": "nickname"
}
```

Response body: `null`

### 로그인

`POST /api/auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Response body:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "nickname": "nickname"
}
```

### 토큰 재발급

`POST /api/auth/reissue`

Header:

```text
Authorization-Refresh: Bearer <refresh-token>
```

Response body:

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

### 로그아웃

`POST /api/auth/logout`

Header:

```text
Authorization: Bearer <access-token>
```

Response body: `null`

## Room API

### 채팅방 생성

`POST /api/rooms`

Request:

```json
{
  "title": "room title",
  "maxMembers": 10
}
```

Validation:

- `title`: blank 불가
- `maxMembers`: 2 이상

Response body:

```json
{
  "roomId": 1,
  "title": "room title",
  "maxMembers": 10,
  "currentMembers": 1,
  "managerNickname": "nickname",
  "createdAt": "2026-04-25T12:00:00"
}
```

### 채팅방 목록 조회

`GET /api/rooms?page=0&size=20&sort=id,desc`

기본 페이징은 `size=20`, `sort=id,DESC`입니다.

Response body: Spring Data `Page` 형태의 방 목록

### 채팅방 상세 조회

`GET /api/rooms/{roomId}`

Response body: `RoomResponse`

### 채팅방 수정

`PATCH /api/rooms/{roomId}`

Request:

```json
{
  "title": "new title",
  "maxMembers": 20
}
```

권한: 방 매니저만 수정 가능해야 합니다.

Response body: `RoomResponse`

### 채팅방 삭제

`DELETE /api/rooms/{roomId}`

권한: 방 매니저만 삭제 가능해야 합니다.

Response body: `null`

### 채팅방 입장

`POST /api/rooms/{roomId}/enter`

Response body: `RoomResponse`

### 채팅방 퇴장

`POST /api/rooms/{roomId}/leave`

Response body: `null`

### 매니저 위임

`PATCH /api/rooms/{roomId}/manager?newManagerId={userId}`

권한: 현재 매니저만 위임 가능해야 합니다.

Response body: `null`

## Chat History API

### 채팅 이력 조회

`GET /api/chat/messages/{roomId}?cursorId={messageId}&size=100`

Query:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `cursorId` | 아니오 | 없음 | 다음 페이지 시작 기준이 되는 Mongo message id |
| `size` | 아니오 | `100` | 조회할 메시지 수 |

Response body:

```json
{
  "messages": [
    {
      "messageId": "...",
      "roomId": 1,
      "senderId": 10,
      "senderNickname": "nickname",
      "content": "hello",
      "createdAt": "2026-04-25T12:00:00"
    }
  ],
  "nextCursorId": "...",
  "hasNext": true
}
```

## WebSocket/STOMP 계약

### 연결

SockJS endpoint:

```text
/ws
```

CONNECT native header:

```text
Authorization: Bearer <access-token>
```

### 구독

```text
/sub/chat/{roomId}
```

구독 시 사용자가 해당 채팅방 참여자인지 검증합니다.

### 메시지 발행

Destination:

```text
/pub/chat/message
```

Payload:

```json
{
  "roomId": 1,
  "content": "hello",
  "type": "CHAT"
}
```

발행 시 인증 사용자와 `roomId`를 기준으로 참여 여부를 검증하고, 메시지는 Redis 브로커 경로를 통해 저장 및 팬아웃됩니다.

### 메시지 전달

서버는 방 구독자에게 다음 destination으로 메시지를 전송합니다.

```text
/sub/chat/{roomId}
```

전달 payload는 `MessageResponse` 구조를 따릅니다.

## 저장소 계약

### MySQL/JPA

- `User`: 계정 정보와 현재 입장한 방 관계를 저장합니다.
- `Room`: 방 제목, 최대 인원, 매니저, 참여자 관계를 저장합니다.

### MongoDB

- 채팅 메시지는 MongoDB에 영속 저장합니다.
- 이력 조회는 Mongo `_id` 기반 커서를 사용합니다.

### Redis

- 로그아웃된 access token을 블랙리스트로 관리합니다.
- Redis Streams 계열 경로는 메시지 저장 처리를 담당합니다.
- Redis Pub/Sub 계열 경로는 다중 인스턴스 fanout을 담당합니다.
- CacheManager 기본 TTL은 10분입니다.

## 비기능 요구사항

- HTTP API는 stateless 인증을 유지합니다.
- Controller에 비즈니스 로직을 두지 않습니다.
- 기능 구현 시 테스트를 추가하고 `gradlew test` 통과를 완료 기준으로 삼습니다.
- 엔드포인트, DTO, 저장소 스키마, 인증 정책 변경 시 `README.md`, `docs/SPEC.md`, 관련 세부 문서를 함께 갱신합니다.
