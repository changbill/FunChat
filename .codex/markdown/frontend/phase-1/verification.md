# Phase 1 검증

- npm run lint 통과.
- npm run build 통과. 기존 stompjs의 net externalization 경고와 영상 SDK chunk 500kB 초과 경고가 남는다. 영상 chunk는 지연 로딩된다.
- npm 설치는 최초 캐시 권한 문제와 이후 ECONNRESET로 실패했다. 권한을 확보하고 동시 다운로드를 1개로 제한한 재시도로 설치 완료했다.
- Chrome + 격리 UI fixture에서 로그인, 일반/영상 필터, 빈 제목 생성 비활성화, 영상 유형 선택/생성/입장, 새로고침 후 유형 유지, 방 퇴장, 일반방의 영상 UI 부재를 확인했다.
- 영상 토큰 502 오류 표시 후 재시도 버튼이 활성 상태임을 확인했다. STOMP 연결 실패 시 전송 비활성화를 확인했다.
- 390x844 모바일 viewport에서 목록/영상방을 시각 검토하고 가로 넘침이 없음을 확인했다.
- 실제 LiveKit 연결 성공, 두 기기 카메라/마이크/화면 공유, 권한 거부 후 장치 재활성화는 운영 미디어 서버에서 추가 확인해야 한다. UI fixture에는 실제 STOMP/LiveKit 서버가 없다.
- 탭 종료 presence 정리는 후속 작업이며 명시적으로 방 나가기를 사용해야 한다.
- 구현 커밋 2d2695d를 feature/3-chat-room-types에 푸시했다. GitHub 연결의 PR 생성은 403(Resource not accessible by integration)으로 실패했고 gh도 미인증이므로 PR 생성은 미완료다.
