# 배포 조사

## Phase 1 — 상시 슬롯 축소

슬롯 범위와 surge 번호는 `APP_REPLICAS`에서 계산된다. 기본값과 정적 upstream을 바꾸면 기존 정리 로직으로 3개 구성에서 2개 구성으로 전환할 수 있다. 4번 Compose 서비스를 유지해야 기존 컨테이너도 `compose rm`으로 정리할 수 있다.
