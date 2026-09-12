# 프런트엔드 조사

## Phase 1 — 일반·영상 채팅방

- 방 유형은 backend에서 영속 저장하고 필터링한다. localStorage나 제목 접두사로 유형을 추정하지 않는다.
- LiveKit 공식 React 컴포넌트를 사용하고 영상방에서 지연 로딩한다. 기존 STOMP 채팅은 유지한다.
- 참고: https://docs.livekit.io/reference/components/react/
