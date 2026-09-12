# 일반·영상 채팅방 조사

- 기존 Room에는 유형이 없어 backend에 영속 유형과 서버 필터를 추가해야 한다.
- LiveKit 공식 React 컴포넌트로 참가자 타일, 트랙 제어, 오디오 재생을 구성한다.
- https://docs.livekit.io/reference/components/react/
- 기존 STOMP 텍스트 채팅을 유지하여 LiveKit data channel과 메시지 저장 경로가 중복되지 않도록 한다.
