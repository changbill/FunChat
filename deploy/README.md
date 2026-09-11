# 배포

Jenkins는 `scripts/jenkins-remote-deploy.sh`를 통해 원격 `deploy.sh`를 실행한다.
상시 슬롯 수와 임시 슬롯 동작은 [SPEC.md](SPEC.md)를 참고한다.

직접 실행 시 Docker 자격 증명과 `ENV_FILE`을 설정하고 `bash deploy/deploy.sh`를 실행한다.
기존 3개 구성도 다음 배포 시 사용하지 않는 슬롯을 정리한다.
