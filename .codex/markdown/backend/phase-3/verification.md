# Phase 3 검증

- 관련 Room/Video 회귀 테스트 통과 후 추가 404 테스트를 포함해 `gradlew.bat test build --no-daemon` 실행.
- 전체 105개 테스트 통과, 실패 0, 스킵 0. bootJar/build 성공.
- H2 저장/재조회 및 legacy null 유형, 필터 페이지 수, DTO 이전 요청 호환, 잘못된 유형/누락 정원 400, 일반방 영상 토큰 거부, 마지막 영상 참여자 퇴장과 세션 삭제, LiveKit 404 멱등 처리를 검증했다.
- 운영 MySQL schema update, 실제 LiveKit room 삭제/미디어 연결은 운영 환경에서 추가 확인해야 한다.
- feature/3-chat-room-types 푸시 완료. PR 생성은 GitHub 연결 403 및 gh 미인증으로 미완료다.
