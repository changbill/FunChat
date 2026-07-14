# Implementation

- `io.livekit:livekit-server:0.13.0` 의존성 추가
- `LiveKitRoomAdminClient` room 삭제 경계 추가
- 영상 세션 종료 전 LiveKit room 삭제 호출
- LiveKit 호출 실패 시 `VIDEO_ROOM_CLEANUP_FAILED` 응답과 `ACTIVE` 상태 유지

## 변경 파일

- `backend/build.gradle`
- `backend/src/main/java/com/funchat/demo/video/service/LiveKitRoomAdminClient.java`
- `backend/src/main/java/com/funchat/demo/video/service/LiveKitRoomAdminClientImpl.java`
- `backend/src/main/java/com/funchat/demo/video/service/VideoService.java`
- `backend/src/main/java/com/funchat/demo/global/exception/ErrorCode.java`
- `backend/src/test/java/com/funchat/demo/video/service/VideoServiceTest.java`
- `backend/src/test/java/com/funchat/demo/video/service/LiveKitRoomAdminClientImplTest.java`
- `PLAN.md`
- `backend/PLAN.md`
- `backend/RESEARCH.md`
- `backend/SPEC.md`

## 설계 판단

- 외부 API 성공 후 DB 세션을 종료한다.
- 외부 API 실패는 자동 재시도하지 않고 관리자의 종료 재요청으로 복구한다.
- `LIVEKIT_URL`이 WebSocket scheme이면 Admin API용 HTTP scheme으로 변환한다.

## 영향 범위

- 방 매니저의 세션 종료 요청은 실제 LiveKit room과 참가자 연결을 정리한다.
- LiveKit 장애 시 FunChat 세션 상태가 종료로 잘못 기록되지 않는다.
