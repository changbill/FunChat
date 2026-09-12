# 배포 계획

## Phase 1 — 상시 슬롯 축소

- [x] Jenkins 및 직접/원격 배포 기본값을 2로 통일한다.
- [x] 정적 upstream에서 3번 슬롯을 제외한다.
- [x] 기존 슬롯 정리와 surge 동작을 유지한다.

검증:
- [x] Git Bash 문법 및 Compose 구성 검사.
- [x] 변경 diff와 슬롯 설정 일관성 확인.
- [ ] 운영 서버의 최초 전환 및 재배포 확인(실제 배포 시 수행).

PR 생성은 GitHub CLI 미인증으로 미완료다.

## Phase 2 — LiveKit 배포 환경변수 전달

- [x] 롤링 앱 템플릿에 LiveKit 환경변수 4개를 필수값으로 전달한다.
- [x] Jenkins 환경 파일 설정 방법과 누락 시 동작을 문서화한다.

검증:
- [x] Compose에서 모든 슬롯의 값 전달 및 누락/빈 값 거부 확인.
- [ ] 운영 Jenkins 환경 파일 확인 후 재배포(운영 환경에서 수행).

Phase 2 PR 생성도 GitHub CLI 미인증으로 미완료다.

## Phase 3 — LiveKit 시크릿 교체와 추적 파일 제거

- [ ] `deploy/livekit.yml`에서 추적된 `keys`의 API 시크릿을 제거하고, 안전한 런타임 주입 방식으로 바꾼다.
- [ ] 새 LiveKit API key/secret 쌍을 생성해 LiveKit 서버와 Jenkins `funchat-env`에 동시에 반영한다.
- [ ] 기존 키/시크릿을 폐기하고 Git 이력에 남은 값이 운영에서 더 이상 유효하지 않음을 확인한다.
- [ ] LiveKit 컨테이너 재생성, backend 재배포, 실제 영상 참가와 기존 토큰 거부를 확인한다.

검증:
- [ ] 추적 파일과 빌드 산출물에 LiveKit API 시크릿이 없는지 검사한다.
- [ ] 새 API key로 참가 토큰 발급 및 두 클라이언트의 영상·음성 연결을 확인한다.
- [ ] 이전 API key로 서명된 토큰이 LiveKit에서 401로 거부되는지 확인한다.
