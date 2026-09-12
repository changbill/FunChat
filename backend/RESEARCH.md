# FunChat Backend Research

## Phase 3 — 채팅방 유형

- nullable enum 컬럼과 TEXT fallback으로 기존 행 및 이전 앱 삽입과 호환한다. 필터는 DB 페이지네이션 전에 수행한다.
- 영상 세션 FK를 정리한 뒤 방을 삭제하며, 일반방 영상 API는 거부한다.

## Phase 2 — LiveKit Admin API

- LiveKit 공식 Java/Kotlin 서버 SDK의 `RoomServiceClient.deleteRoom`으로 영상 세션 종료 시 실제 room을 삭제한다.
- 외부 호출 실패 시 DB 세션을 `ACTIVE`로 유지하고 자동 재시도 대신 관리자 재요청을 허용한다.
- 상세: [Phase 2 research](../.codex/markdown/backend/phase-2/research.md)
