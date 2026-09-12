# FunChat Frontend

React + Vite 기반 일반/영상 채팅 서비스다. 일반방은 STOMP 텍스트 채팅, 영상방은 텍스트 채팅과 LiveKit 통화를 제공한다.

## 실행 및 검증

```sh
npm ci
npm run dev
npm run lint
npm run build
```

개발 서버는 `/api`, `/ws`를 `localhost:8080`의 backend로 프록시한다. 운영에서는 기존 Nginx 동일 출처 경로를 사용한다. 프런트엔드에 LiveKit API secret을 넣지 않는다. 영상 참가 토큰과 공개 URL은 백엔드에서 받는다.

UI만 확인할 때는 `node tests/preview-server.mjs`로 격리된 테스트 API와 Vite(5173)를 실행한다. 임의의 테스트 이메일/비밀번호로 로그인한다. 이 fixture는 실제 STOMP/LiveKit 서버를 구현하지 않으며 채팅 연결 실패와 영상 토큰 502 오류를 의도적으로 표시한다. 운영 서버나 실제 계정에는 연결하지 않는다.

## 영상통화 확인

backend의 채팅방 유형 API와 함께 배포한다. 새 영상방을 생성하고 두 계정으로 입장한 뒤 `영상통화 참가`를 누른다. 카메라·마이크는 처음에는 꺼져 있으며 버튼을 눌러 켠다. 브라우저는 HTTPS(로컬은 localhost), 장치 권한, 공개 LiveKit signaling 주소와 미디어 포트 연결이 필요하다.

영상 SDK는 영상방에서만 지연 로딩된다. 동작 계약과 제한은 [SPEC.md](SPEC.md), 검증 결과는 [Phase 1 기록](../.codex/markdown/frontend/phase-1/verification.md)에 정리한다.
