# 배포 슬롯 명세

- Jenkins와 두 배포 스크립트의 `APP_REPLICAS` 기본값은 `2`다.
- backend와 frontend는 각각 1, 2번 슬롯을 상시 사용한다.
- 배포 중 각 영역의 3번 슬롯을 surge로 사용하고, 완료하면 제거한다.
- 정적 Nginx upstream 목록도 각각 1, 2번 슬롯만 포함한다.
- `MAX_APP_REPLICAS=3`, `MAX_ROLLING_SLOTS=4`와 Compose의 4개 슬롯 정의는 유지한다. 명시적인 확장 설정 및 기존 3, 4번 슬롯 정리에 사용한다.
- 기존 구성에서 전환할 때 1, 2번 readiness 확인과 upstream reload 후 사용하지 않는 3, 4번 슬롯을 제거한다.
