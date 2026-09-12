# Phase 3 조사 — LiveKit 시크릿 노출

- `deploy/livekit.yml`은 Git이 추적하는 운영 설정 템플릿이며 `keys.devkey`의 값으로 API 시크릿을 평문 보관한다.
- LiveKit API key는 토큰의 `iss` claim에 포함되는 식별자여서 비밀이 아니다. API secret은 HMAC 참가 토큰 서명 키여서 유출 시 임의의 참가 권한 토큰을 만들 수 있다.
- 현재 파일에 기록된 시크릿은 Git 이력에도 남아 있다. 단순히 파일을 수정하는 것만으로 과거 이력의 복사본을 제거할 수 없으므로 반드시 새 key/secret 쌍으로 회전해야 한다.
- 서버의 LiveKit 설정과 Jenkins `funchat-env`는 같은 신규 쌍을 사용해야 하며, 서버 재생성 뒤 backend를 재배포해야 한다.
