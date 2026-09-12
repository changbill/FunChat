# 배포 조사

## Phase 1 — 상시 슬롯 축소

슬롯 범위와 surge 번호는 `APP_REPLICAS`에서 계산된다. 기본값과 정적 upstream을 바꾸면 기존 정리 로직으로 3개 구성에서 2개 구성으로 전환할 수 있다. 4번 Compose 서비스를 유지해야 기존 컨테이너도 `compose rm`으로 정리할 수 있다.

## Phase 2 — LiveKit 환경변수 전달

롤링 Compose의 앱 환경변수 목록에서 LiveKit 설정 4개가 누락되어 backend의 TTL 바인딩이 실패했다. 공통 앱 템플릿에 필수값 전달을 추가했다. Jenkins `funchat-env` 내용과 운영 재배포 결과는 별도 확인이 필요하다.

## Phase 3 — LiveKit 시크릿 노출

`deploy/livekit.yml`은 Git 추적 파일이며 `keys`에 API 시크릿을 평문으로 포함한다. API key는 식별자라 참가 토큰의 issuer에도 들어가지만, API secret은 토큰을 위조할 수 있으므로 저장소와 로그에 남겨서는 안 된다. 기존 값은 교체하고, LiveKit 서버 설정과 Jenkins 환경 파일을 새 쌍으로 동시에 전환해야 한다.
