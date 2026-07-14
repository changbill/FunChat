# FunChat Backend Research

## Phase 2 — LiveKit Admin API

- LiveKit 공식 Java/Kotlin 서버 SDK의 `RoomServiceClient.deleteRoom`으로 영상 세션 종료 시 실제 room을 삭제한다.
- 외부 호출 실패 시 DB 세션을 `ACTIVE`로 유지하고 자동 재시도 대신 관리자 재요청을 허용한다.
- 상세: [Phase 2 research](../.codex/markdown/backend/phase-2/research.md)
