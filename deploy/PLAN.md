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
