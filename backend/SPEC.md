# FunChat Backend Specification

이 문서는 FunChat 백엔드의 현재 기능 계약을 정의합니다. 구현 변경 시 API, 데이터 흐름, 인증 정책, 저장소 정책이 바뀌면 이 문서를 함께 갱신합니다.

## 서비스 범위

FunChat 백엔드는 다음 책임을 가집니다.

- 사용자 계정 생성, 로그인, 토큰 재발급, 로그아웃
- JWT 기반 HTTP API 인증
- STOMP native header 기반 WebSocket 인증
- 채팅방 생성, 조회, 수정, 삭제
- 채팅방 입장/퇴장 및 매니저 위임
- 채팅 메시지 실시간 전송
- 채팅 메시지 영속 저장 및 이력 조회
- 방 단위 영상 세션 생성/조회/종료와 LiveKit 참가 토큰 발급
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

실패 응답 예시:

```json
{
  "code": 401,
  "message": "유효하지 않은 Access 토큰입니다.",
  "body": null
}
```

검증 실패처럼 상세 원인이 필요한 경우 `body`에 검증 상세 객체가 포함될 수 있습니다.

대표 에러 코드:

| HTTP status | 메시지                              | 대표 상황                                       |
| ----------- | ----------------------------------- | ----------------------------------------------- |
| 400         | 요청 값이 올바르지 않습니다.        | 요청 DTO 검증 실패, JSON 파싱 실패              |
| 401         | 액세스 토큰이 존재하지 않습니다.    | 인증 필요 API에 access token 누락               |
| 401         | 유효하지 않은 Access 토큰입니다.    | 변조된 access token 또는 잘못된 token type      |
| 401         | 이미 로그아웃된 토큰입니다.         | Redis 블랙리스트에 등록된 access token 사용     |
| 403         | 방장만 수행할 수 있는 작업입니다.   | 방 수정/삭제/위임 권한 없음                     |
| 403         | 해당 채팅방의 참여자가 아닙니다.    | STOMP 구독/전송 또는 매니저 위임 대상 검증 실패 |
| 404         | 요청한 채팅방을 찾을 수 없습니다.   | 존재하지 않는 roomId 조회 또는 입장             |
| 404         | 요청한 사용자를 찾을 수 없습니다.   | 존재하지 않는 userId 사용                       |
| 404         | 활성 영상 세션을 찾을 수 없습니다.  | 활성 영상 세션 조회/종료 시 세션 없음           |
| 409         | 채팅방 인원이 가득 찼습니다.        | 정원 초과 입장                                  |
| 409         | 이미 참여 중인 채팅방입니다.        | 같은 방 또는 다른 방 중복 입장                  |
| 500         | 메시지 전송에 실패했습니다.         | Redis Streams 저장 경로 발행 실패               |
| 500         | 영상 서버 설정이 올바르지 않습니다. | LiveKit API key/secret/url 설정 누락            |
| 502         | 영상 방 정리에 실패했습니다.        | LiveKit Admin API room 삭제 실패                |

### 인증 헤더

| 목적          | Header                  | 형식                     |
| ------------- | ----------------------- | ------------------------ |
| Access token  | `Authorization`         | `Bearer <access-token>`  |
| Refresh token | `Authorization-Refresh` | `Bearer <refresh-token>` |

`/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout`, `/ws`, `/health`, `/actuator/**`를 제외한 HTTP API는 인증을 요구합니다.

## Health API

### 애플리케이션 헬스 체크

`GET /health`

인증: 필요 없음

정상 응답:

```json
{
  "code": 200,
  "message": "성공",
  "body": {
    "status": "UP",
    "checks": {
      "database": "UP",
      "redis": "UP",
      "mongodb": "UP"
    }
  }
}
```

MySQL, Redis, MongoDB 중 하나라도 준비되지 않으면 HTTP 503을 반환합니다. 배포 compose healthcheck는 이 엔드포인트의 2xx 응답을 기준으로 신규 app 컨테이너의 준비 상태를 판단합니다.

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

정책:

- 정원을 초과해 입장할 수 없습니다.
- 이미 채팅방에 참여 중인 사용자는 같은 방이나 다른 방에 중복 입장할 수 없습니다.
- 강퇴된 사용자는 해당 방에 다시 입장할 수 없습니다.

Response body: `RoomResponse`

### 채팅방 퇴장

`POST /api/rooms/{roomId}/leave`

정책:

- 일반 참여자가 퇴장하면 방은 유지되고 참여자 목록에서만 제거됩니다.
- 매니저가 퇴장하면 남은 참여자 중 한 명에게 매니저를 위임합니다.
- 마지막 참여자가 퇴장하면 방을 삭제합니다.

Response body: `null`

### 매니저 위임

`PATCH /api/rooms/{roomId}/manager?newManagerId={userId}`

정책:

- 현재 매니저만 위임할 수 있습니다.
- 위임 대상은 해당 방의 현재 참여자여야 합니다.

Response body: `null`

## Video API

영상 API는 기존 채팅방 참여자만 사용할 수 있습니다. FunChat 백엔드는 영상 세션 상태와 권한을 관리하고, 실제 WebRTC signaling/media 처리는 LiveKit이 담당합니다.

### 영상 세션 시작

`POST /api/rooms/{roomId}/video/sessions`

정책:

- 인증 사용자가 해당 방 참여자여야 합니다.
- 이미 ACTIVE 상태의 세션이 있으면 새로 만들지 않고 기존 세션을 반환합니다.
- LiveKit room 이름은 `funchat-room-{roomId}` 형식을 사용합니다.

Response body:

```json
{
  "sessionId": 1,
  "roomId": 10,
  "livekitRoomName": "funchat-room-10",
  "status": "ACTIVE",
  "startedAt": "2026-05-12T12:00:00",
  "endedAt": null
}
```

### 활성 영상 세션 조회

`GET /api/rooms/{roomId}/video/session`

정책:

- 인증 사용자가 해당 방 참여자여야 합니다.
- ACTIVE 상태의 세션이 없으면 `VIDEO_SESSION_NOT_FOUND` 에러를 반환합니다.

Response body: `VideoSessionResponse`

### LiveKit 참가 토큰 발급

`POST /api/rooms/{roomId}/video/token`

정책:

- 인증 사용자가 해당 방 참여자여야 합니다.
- ACTIVE 세션이 없으면 세션을 생성한 뒤 토큰을 발급합니다.
- 토큰 identity는 `user-{userId}` 형식을 사용합니다.
- 토큰은 LiveKit JWT이며 `video.roomJoin=true`, `canPublish=true`, `canPublishData=true`, `canSubscribe=true` grant를 포함합니다.
- 토큰 만료 시간은 `LIVEKIT_TOKEN_TTL_SECONDS`를 따르며 기본값은 3600초입니다.

Response body:

```json
{
  "sessionId": 1,
  "roomId": 10,
  "livekitUrl": "http://localhost:7880",
  "livekitRoomName": "funchat-room-10",
  "identity": "user-1",
  "participantName": "nickname",
  "token": "...",
  "expiresAt": "2026-05-12T13:00:00"
}
```

### 영상 세션 종료

`POST /api/rooms/{roomId}/video/sessions/{sessionId}/end`

정책:

- 인증 사용자가 해당 방 매니저여야 합니다.
- 지정한 `sessionId`가 해당 방의 ACTIVE 세션이어야 합니다.
- 성공 시 세션 상태를 `ENDED`로 변경하고 `endedAt`을 기록합니다.
- LiveKit Admin API로 실제 room을 삭제해 연결된 참가자를 퇴장시킨 뒤 FunChat DB 세션을 종료합니다.
- LiveKit 호출이 실패하면 `502 영상 방 정리에 실패했습니다.`를 반환하고 DB 세션은 `ACTIVE`로 유지합니다. 자동 재시도하지 않으며 관리자가 종료 요청을 다시 실행할 수 있습니다.

Response body: `VideoSessionResponse`

## Chat History API

### 채팅 이력 조회

`GET /api/chat/messages/{roomId}?cursorId={messageId}&size=100`

Query:

| 이름       | 필수   | 기본값 | 설명                                          |
| ---------- | ------ | ------ | --------------------------------------------- |
| `cursorId` | 아니오 | 없음   | 다음 페이지 시작 기준이 되는 Mongo message id |
| `size`     | 아니오 | `100`  | 조회할 메시지 수                              |

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
  "content": "hello"
}
```

발행 시 인증 사용자와 `roomId`를 기준으로 참여 여부를 검증하고, 메시지는 Redis 브로커 경로를 통해 저장 및 팬아웃됩니다.

메시지 타입 정책:

- 클라이언트가 발행한 일반 채팅은 서버에서 `TEXT` 타입으로 저장 및 전파합니다.
- `JOIN`, `LEAVE`, `DELEGATE`, `BAN`은 서버가 생성하는 시스템 메시지에만 사용합니다.
- 시스템 메시지 발신자는 `senderId=0`, `senderNickname=SYSTEM`으로 표시합니다.

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
- `VideoSession`: 방별 LiveKit room 이름, 세션 상태(`ACTIVE`, `ENDED`), 시작/종료 시각을 저장합니다.

### MongoDB

- 채팅 메시지는 MongoDB에 영속 저장합니다.
- 저장 경로는 `ChatService.saveMessageToMongo(...)`를 따릅니다.
- 이력 조회는 Mongo `_id` 기반 커서를 사용합니다.
- 메시지 조회는 `_id` 내림차순 정렬과 `idLessThan(cursorId)` 형태의 커서 기반 슬라이스 조회를 사용합니다.
- 채팅 이력 조회 패턴에 맞춰 `roomId ASC, _id DESC` 복합 인덱스(`idx_chat_message_room_id_id_desc`)를 유지합니다.

### Redis

- 로그아웃된 access token을 블랙리스트로 관리합니다.
- Redis Streams 계열 경로는 메시지 저장 처리를 담당합니다.
- Redis Pub/Sub 계열 경로는 다중 인스턴스 fanout을 담당합니다.
- `RedisCacheConfig`의 CacheManager 기본 TTL은 10분입니다.
- 캐시 key는 문자열, value는 JSON 기반 직렬화를 사용합니다.

장애 처리 정책:

- Streams 저장 경로 발행에 실패하면 메시지 전송은 실패로 처리하고 Pub/Sub 팬아웃을 수행하지 않습니다.
- Pub/Sub 팬아웃 발행 실패는 실시간 전달 실패로 기록하되, 이미 성공한 저장 경로 발행은 되돌리지 않습니다.
- Streams 소비 중 Mongo 저장에 실패하면 ack하지 않고 pending 상태로 남겨 재처리/claim 정책의 대상이 되게 합니다.

## 보안 요구사항

- HTTP API는 stateless 인증을 유지합니다.
- HTTP 인증은 `Authorization: Bearer <access-token>` 정책을 유지합니다.
- Refresh token은 `Authorization-Refresh: Bearer <refresh-token>` 정책을 유지합니다.
- STOMP 인증은 native header `Authorization` 정책을 유지합니다.
- 로그아웃된 access token은 Redis 블랙리스트로 무효화합니다.
- 인증 실패와 접근 제어 실패는 상세 메시지를 최소화합니다.

## 외부 연동

### LiveKit

- 클라이언트는 FunChat JWT로 백엔드 `POST /api/rooms/{roomId}/video/token`을 호출해 LiveKit 참가 토큰을 받습니다.
- 클라이언트는 응답의 `livekitUrl`, `livekitRoomName`, `token`으로 LiveKit SDK에 접속하고 카메라/마이크/화면 트랙을 publish합니다.
- 백엔드는 세션 종료 시 LiveKit Admin API로 room을 삭제합니다. webhook 기반 참가자 이벤트 반영은 후속 작업입니다.
