# Research

## LiveKit Admin API room 삭제

- 공식 SDK: `io.livekit:livekit-server:0.13.0`
- 공식 구현: `RoomServiceClient.createClient(host, apiKey, secret)`와 `deleteRoom(roomName).execute()` 사용
- `deleteRoom`은 room을 종료하고 연결된 참가자를 퇴장시킨다.
- 클라이언트용 `ws://`·`wss://` URL은 Admin API 호출 시 `http://`·`https://`로 변환한다.

## 실패 정책

- 외부 호출 실패 시 DB 세션 종료를 수행하지 않는다.
- 자동 재시도는 적용하지 않는다.
- `502 VIDEO_ROOM_CLEANUP_FAILED`로 응답하고 관리자가 동일 종료 요청을 다시 실행한다.

## 근거

- LiveKit Java/Kotlin server SDK: https://github.com/livekit/server-sdk-kotlin
- RoomServiceClient 구현: https://github.com/livekit/server-sdk-kotlin/blob/main/src/main/kotlin/io/livekit/server/RoomServiceClient.kt
