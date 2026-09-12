# Phase 1 구현

- 목록에 전체/일반/영상 필터, 유형 뱃지, 제목·최대 인원·유형 생성 폼을 추가했다.
- RoomPage는 서버 입장 응답으로 유형을 확인하며 같은 방 새로고침을 지원한다.
- 영상 패널은 LiveKitRoom, GridLayout, ParticipantTile, RoomAudioRenderer, StartAudio, TrackToggle을 사용한다. 미디어 기본 off, 참가 토큰 메모리 보관, 통화 종료/재참가, 네트워크/장치 오류 안내를 제공한다.
- 기존 STOMP 경로를 유지하고 연결 실패 표시와 전송 비활성화, 한글 조합 중 Enter 전송 방지를 추가했다.
- 모바일 레이아웃, 한국어 문서 언어 및 기본 스타일을 정리했다.
- `tests/preview-server.mjs`로 운영과 분리된 UI fixture를 제공한다.
