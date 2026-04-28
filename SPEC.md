# FunChat Specification

기준일: 2026-04-28

## 배포 기능

### 운영 배포

- Jenkins는 백엔드와 프론트엔드 Docker 이미지를 빌드해 Docker Hub에 push한 뒤, 미니PC의 `~/funchat/deploy`로 `deploy/` 산출물을 동기화한다.
- Jenkinsfile은 반복 shell 절차를 직접 갖지 않고 `deploy/scripts/docker-push-images.sh`, `deploy/scripts/jenkins-remote-deploy.sh`를 호출한다.
- `deploy/deploy.sh`는 credential 로딩, 임시 파일 cleanup, Docker login/logout, HTTP 요청 공통 함수를 `deploy/scripts/deploy-common.sh`에서 가져온다.
- 운영 배포 스크립트는 `deploy/deploy.sh`다.
- 운영 앱 배포는 `deploy/docker-compose.rolling.yml`의 고정 슬롯을 사용한다.
- 상시 지원 슬롯은 현재 최대 3개이고, 배포 중 임시 surge 슬롯 1개를 추가로 사용한다.
  - backend: `funchat-app-1`, `funchat-app-2`, `funchat-app-3`
  - frontend: `funchat-web-1`, `funchat-web-2`, `funchat-web-3`
  - surge: `funchat-app-4`, `funchat-web-4`
- `APP_REPLICAS`는 실제로 사용할 슬롯 수를 의미한다. 기본값은 `3`이다.
- `MAX_APP_REPLICAS` 기본값은 `3`, `MAX_ROLLING_SLOTS` 기본값은 `4`다.

### 롤링 교체 동작

- 배포 스크립트는 Docker Hub 로그인 후 infra compose를 먼저 기동 또는 유지한다.
- 새 backend/frontend 이미지를 pull한다.
- 누락된 슬롯 컨테이너가 있으면 생성하고 상태를 확인한다.
- Nginx upstream 블록과 공통 설정은 `deploy/nginx/upstream.conf`에 둔다.
- `deploy.sh`는 라우팅 대상 서버 목록만 `deploy/nginx/upstream.web.servers.conf`, `deploy/nginx/upstream.app.servers.conf`에 갱신한다.
- `APP_REPLICAS`보다 큰 번호의 슬롯은 평상시 upstream에서 제외되고 중지된다.
- backend 배포 시작 시 surge backend 슬롯(`app-{APP_REPLICAS+1}`)을 새 이미지로 한 번 생성한다.
- surge backend 슬롯은 `/health`가 healthy가 되어야 upstream에 투입된다.
- surge backend 슬롯은 모든 backend 슬롯 교체가 끝날 때까지 upstream에 유지된다.
- 각 backend 슬롯 교체는 기존 `app-N`을 upstream에서 제외하고 재생성한 뒤, 새 `app-N`이 healthy가 되면 upstream에 재투입한다.
- 모든 backend 슬롯 교체가 끝나면 surge backend 슬롯은 upstream에서 제외한 뒤 중지한다.
- backend 배포 실패 시에는 가용성을 우선해 surge backend 슬롯을 upstream에 남기고, 진행 중이던 문제 슬롯은 제외한다.
- frontend 배포 시작 시 surge frontend 슬롯(`web-{APP_REPLICAS+1}`)을 새 이미지로 한 번 생성한다.
- surge frontend 슬롯은 running 상태가 되어야 upstream에 투입된다.
- surge frontend 슬롯은 모든 frontend 슬롯 교체가 끝날 때까지 upstream에 유지된다.
- 각 frontend 슬롯 교체는 기존 `web-N`을 upstream에서 제외하고 재생성한 뒤, 새 `web-N`이 running 상태가 되면 upstream에 재투입한다.
- 모든 frontend 슬롯 교체가 끝나면 surge frontend 슬롯은 upstream에서 제외한 뒤 중지한다.
- frontend 배포 실패 시에는 가용성을 우선해 surge frontend 슬롯을 upstream에 남기고, 진행 중이던 문제 슬롯은 제외한다.
- 이 방식은 배포 중 특정 시점의 라우팅 대상 수가 `APP_REPLICAS`보다 줄어드는 것을 피한다.
- 각 슬롯 교체 후 Nginx 경유 smoke test 대상은 기본적으로 `/health`, `/`이다.

### 라우팅

- `deploy/docker-compose.router.yml`은 `funchat-router` Nginx 컨테이너를 실행한다.
- Nginx는 `/health`, `/api/`, `/ws`를 backend upstream으로 프록시한다.
- 그 외 경로 `/`는 frontend upstream으로 프록시한다.
- Nginx 컨테이너는 배포 중 재생성하지 않고 설정 테스트 후 reload한다.

### 외부 연동과 제한사항

- MySQL, MongoDB, Redis는 `deploy/docker-compose.infra.yml`로 운영한다.
- LiveKit은 `deploy/docker-compose.livekit.yml`로 별도 실행하며 앱 롤링 배포에 포함되지 않는다.
- Jenkins의 `funchat-env` credential 파일은 배포 시 원격 `/tmp/funchat-env.XXXXXX` 임시 파일로 전송된다.
- 원격 env 임시 파일과 Docker Hub credential 임시 파일은 `umask 077`이 적용된 `mktemp`로 생성하고, Jenkins 단계 종료 및 `deploy.sh` 종료 시 삭제를 시도한다.
- Docker Hub 비밀번호는 SSH 원격 실행 명령줄 인자로 전달하지 않고, 별도 임시 파일의 두 번째 줄로 전달한다.
- 원격 `docker login` 이후 배포가 중간 실패해도 `deploy.sh`의 cleanup trap이 `docker logout`을 시도한다.
- 현재 운영 이미지는 `latest` 태그를 사용한다. 실패 슬롯을 이전 이미지로 완전 자동 rollback하는 기능은 제공하지 않는다.
- WebSocket 장기 연결의 graceful shutdown은 아직 구현되어 있지 않다.
- blue/green 운영용 compose와 upstream 파일은 현재 배포 경로에서 사용하지 않는다.
