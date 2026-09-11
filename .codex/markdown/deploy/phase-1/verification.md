# Phase 1 검증

- Git Bash `bash -n deploy/deploy.sh deploy/scripts/jenkins-remote-deploy.sh`: 통과.
- WSL Bash는 `E_ACCESSDENIED`로 실행 실패해 Git Bash로 재검증했다.
- `docker compose -f deploy/docker-compose.rolling.yml config --quiet`: 통과. 로컬 Docker config 접근 권한 경고가 있었다.
- `git diff --check`: 통과.
- 슬롯 계산 검토: 기본 상시 1, 2번, surge 3번이며 기존 정리 범위는 3, 4번이다.
- 실제 서버 전환, Nginx 런타임 검사 및 재배포는 미실행. 운영 배포 환경에서 확인해야 한다.
- 애플리케이션 코드 변경이 없어 frontend/backend 타입 검사, 테스트, 린트, 빌드는 실행하지 않았다.
- `gh auth status`: 로그인된 GitHub 호스트가 없어 PR 생성 미완료.
