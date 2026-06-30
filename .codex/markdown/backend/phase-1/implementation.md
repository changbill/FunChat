# Implementation

- `video` 도메인에 `VideoSession` 엔티티와 repository 추가
- 영상 세션 시작·조회·종료와 LiveKit 참가 토큰 발급 API 추가
- Room 참여자 권한 검증, 매니저 종료 권한 검증, 활성 세션 재사용 구현
- LiveKit 설정과 Docker Compose 환경 변수 추가
- 영상 API 계약과 테스트 전략 문서화

## 변경 파일

- `backend/src/main/java/com/funchat/demo/video/**`
- `backend/src/main/java/com/funchat/demo/global/exception/ErrorCode.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/funchat/demo/video/**`
- `backend/src/test/resources/application-test.yml`
- `deploy/docker-compose.local.yml`
- `PLAN.md`
- `backend/PLAN.md`
- `backend/README.md`
- `backend/SPEC.md`
- `backend/testing.md`

## 설계 판단

- LiveKit 참가 토큰은 기존 `jjwt`로 생성해 신규 SDK 의존성을 추가하지 않았다.
- 활성 영상 세션이 존재하면 기존 세션을 반환한다.
- FunChat 백엔드는 세션 상태와 참가 권한을 관리하고 미디어 전송은 LiveKit에 위임한다.

## 영향 범위

- Room 참여자는 영상 세션을 시작·조회하고 publish/subscribe 권한이 포함된 LiveKit JWT를 발급받을 수 있다.
- Room 매니저는 활성 영상 세션을 종료 상태로 변경할 수 있다.
- LiveKit webhook과 Admin API 기반 실제 LiveKit room 정리는 후속 단계 범위다.
