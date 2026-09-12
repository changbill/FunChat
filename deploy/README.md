# 배포

Jenkins는 `scripts/jenkins-remote-deploy.sh`를 통해 원격 `deploy.sh`를 실행한다.
상시 슬롯 수와 임시 슬롯 동작은 [SPEC.md](SPEC.md)를 참고한다.

직접 실행 시 Docker 자격 증명과 `ENV_FILE`을 설정하고 `bash deploy/deploy.sh`를 실행한다.
기존 3개 구성도 다음 배포 시 사용하지 않는 슬롯을 정리한다.

Jenkins의 `funchat-env` 파일(직접 실행 시 `ENV_FILE`)에는 `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `LIVEKIT_URL`, `LIVEKIT_TOKEN_TTL_SECONDS`가 모두 필요하다. 키/시크릿은 LiveKit 서버 설정과 일치해야 하고 URL은 클라이언트에서 접근 가능한 LiveKit 주소여야 한다. TTL은 초 단위 양의 정수(예: `600`)로 설정한다. 롤링 Compose는 이 값을 모든 앱 슬롯에 전달하며, 누락되거나 비어 있으면 구성 해석 단계에서 실패한다.
