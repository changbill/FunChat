# 배포 조사

## Phase 1 — 상시 슬롯 축소

슬롯 범위와 surge 번호는 `APP_REPLICAS`에서 계산된다. 기본값과 정적 upstream을 바꾸면 기존 정리 로직으로 3개 구성에서 2개 구성으로 전환할 수 있다. 4번 Compose 서비스를 유지해야 기존 컨테이너도 `compose rm`으로 정리할 수 있다.

## Phase 2 — LiveKit 환경변수 전달

롤링 Compose의 앱 환경변수 목록에서 LiveKit 설정 4개가 누락되어 backend의 TTL 바인딩이 실패했다. 공통 앱 템플릿에 필수값 전달을 추가했다. Jenkins `funchat-env` 내용과 운영 재배포 결과는 별도 확인이 필요하다.
