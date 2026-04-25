# API Patterns

## 공통 응답 포맷

모든 HTTP Controller는 공통 응답 DTO를 반환합니다.

- `ResponseDto(code, message, body)`
  - `code`: HTTP status code 값
  - `message`: 기본 성공 메시지는 `"성공"`
  - `body`: 실제 페이로드(없으면 `null`)

권장 사용:

- 성공: `ResponseUtil.createSuccessResponse(body)`
- 실패: (공통 예외 처리 흐름에서) `ResponseUtil.createErrorResponse(...)`

## Controller 작성 패턴

- `@RestController` + `@RequestMapping("/api/...")`
- 입력 DTO는 `@RequestBody`로 받고, 필요한 경우 `@Valid` 적용
- 인증 사용자 정보는 `@AuthenticationPrincipal`로 받음
- Controller에서 컬렉션 필터링/정렬/권한 판단 같은 비즈니스 로직을 구현하지 않음(서비스로 이동)

## 인증 관련 API

`/api/auth/**`는 인증 예외로 열려 있으며(permitAll), 내부에서 JWT 토큰 발급/재발급/로그아웃을 처리합니다.

헤더 관례(코드와 호환 유지):

- access token: `Authorization`
- refresh token: `Authorization-Refresh`

## 페이징/커서 패턴(채팅 메시지)

채팅 메시지 조회는 MongoDB의 `_id`를 기준으로 커서 기반 조회를 사용합니다.

- `GET /api/chat/messages/{roomId}`
  - `cursorId`(optional): 다음 페이지 시작 커서
  - `size`(default 100)

응답은 보통 아래를 포함하는 형태(DTO에 맞춰 유지):

- 메시지 리스트
- `nextCursorId`
- `hasNext`

## WebSocket(STOMP) 패턴

### 엔드포인트/라우팅

- SockJS endpoint: `/ws`
- subscribe prefix: `/sub`
- publish prefix: `/pub`

예시(개념):

- subscribe: `/sub/chat/{roomId}`
- publish: `/pub/...` (클라이언트 전송 목적지)

### 인증/권한

- `CONNECT`: native header의 `Authorization` 토큰으로 인증
- `SUBSCRIBE`: `/sub/chat/{roomId}` 구독 시 “해당 roomId 참여자” 검증
- `SEND`: native header `roomId`를 사용하여 참여자 검증

※ STOMP 규칙을 변경해야 한다면, 클라이언트와의 프로토콜 호환성도 함께 고려해 업데이트합니다.

