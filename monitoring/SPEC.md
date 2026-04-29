# Monitoring Spec

작성일: 2026-04-29

이 문서는 현재 `monitoring` 폴더에 실제로 구현되어 있는 기능만 정의한다. 계획 중인 개선 사항이나 미확정 대안은 `PLAN.md`, 조사 내용은 `RESEARCH.md`에 둔다.

## 목적

`monitoring` 폴더는 미니PC 배포 서버에서 Funchat 백엔드와 데이터베이스 상태를 Prometheus로 수집하고, 로컬 PC에서 실제 사용자 접속 경로인 `https://funchat.changee.cloud`를 대상으로 실행한 k6 부하 테스트 결과를 Prometheus remote write endpoint로 받을 수 있는 Docker Compose 기반 모니터링 구성을 제공한다.

## 구성 파일

- `docker-compose.yml`: Prometheus, Grafana, loadgen profile의 k6, MySQL exporter 서비스를 정의한다.
- `prometheus.yml`: Prometheus scrape job과 remote write 설정을 정의한다.
- `scripts/test.js`: k6 HTTP 부하 테스트 시나리오를 정의한다.
- `.env`: MySQL exporter용 계정명과 비밀번호 값을 보관한다.

## Docker Compose 서비스

### prometheus

- 이미지: `prom/prometheus`
- 포트: 호스트 `9090`을 컨테이너 `9090`에 연결한다.
- 설정 파일: `./prometheus.yml`을 `/etc/prometheus/prometheus.yml`로 마운트한다.
- 네트워크: 외부 Docker network `funchat_net`에 연결한다.
- 실행 옵션:
  - `--config.file=/etc/prometheus/prometheus.yml`
  - `--web.enable-remote-write-receiver`

### grafana

- 이미지: `grafana/grafana`
- 포트: 호스트 `3000`을 컨테이너 `3000`에 연결한다.
- `prometheus` 서비스에 의존한다.
- 네트워크: 외부 Docker network `funchat_net`에 연결한다.
- datasource와 dashboard provisioning은 현재 구성되어 있지 않다.

### k6

- 이미지: `grafana/k6:latest`
- 컨테이너 이름: `k6`
- profile: `loadgen`
- 볼륨: `./scripts`를 `/scripts`로 마운트한다.
- 환경변수:
  - `K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write`
- `prometheus` 서비스에 의존한다.
- 네트워크: 외부 Docker network `funchat_net`에 연결한다.
- 기본 profile에서는 실행되지 않는다. 사용자가 `--profile loadgen` 또는 로컬 PC의 k6 CLI/Docker 명령으로 실행해야 한다.

### mysql-exporter

- 이미지: `prom/mysqld-exporter`
- 컨테이너 이름: `mysql-exporter`
- 포트: 호스트 `9104`를 컨테이너 `9104`에 연결한다.
- MySQL 접속 문자열:
  - `${EXPORTER_ID}:${EXPORTER_PW}@(funchat-mysqldb:3306)/`
- 네트워크: 외부 Docker network `funchat_net`에 연결한다.

## Prometheus 수집 설정

전역 설정:

- `scrape_interval`: `5s`

scrape job:

- `prometheus`
  - target: `prometheus:9090`
  - Prometheus 자체 메트릭을 수집한다.
- `spring-boot-apps`
  - targets:
    - `funchat-app-1:8081`
    - `funchat-app-2:8081`
    - `funchat-app-3:8081`
    - `funchat-app-4:8081`
  - metrics path: `/actuator/prometheus`
  - 배포 Docker network 안의 Spring Boot 앱 슬롯별 Actuator Prometheus 메트릭을 수집한다.
  - 실행 중이지 않은 슬롯은 Prometheus targets 화면에서 `down`으로 보일 수 있다.
- `mysql`
  - target: `funchat-mysql-exporter:9104`
  - metrics path: `/metrics`
  - MySQL exporter가 노출하는 MySQL 메트릭을 수집한다.

remote write receiver:

- Prometheus 컨테이너는 `--web.enable-remote-write-receiver` 옵션을 켜고 실행된다.
- 로컬 PC의 k6는 `K6_PROMETHEUS_RW_SERVER_URL=http://<MINIPC_HOST>:9090/api/v1/write`로 결과를 전송할 수 있다.

## k6 부하 테스트 동작

테스트 대상:

- 기본 URL: `BASE_URL` 환경변수 값
- 사용자 체감 품질 측정 권장 URL: `https://funchat.changee.cloud`
- fallback URL: `http://host.docker.internal:8080`

요청 흐름:

1. 0부터 19 사이의 랜덤 page 값을 선택한다.
2. `GET /api/rooms?page={page}&size=20`을 호출한다.
3. 응답 status가 `200`인지 확인한다.
4. 응답 JSON의 Page `content` 배열이 존재하고 비어 있지 않은지 확인한다.
   - 공통 응답 래퍼가 있으면 `body.content`를 사용한다.
   - 래퍼가 없으면 `content`를 사용한다.
5. 목록 조회 check가 성공하면 응답의 방 목록에서 랜덤 방을 선택한다.
6. 방 식별자는 `roomId`를 우선 사용하고, 없으면 `id`를 사용해 `GET /api/rooms/{id}`를 호출한다.
7. 상세 조회 응답 status가 `200`인지 확인한다.
8. 1초 대기 후 다음 iteration을 실행한다.

부하 단계:

- 30초 동안 50 VU까지 증가
- 1분 동안 50 VU 유지
- 30초 동안 1000 VU까지 증가
- 30초 동안 0 VU까지 감소

threshold:

- `http_req_duration`: p95가 200ms 미만이어야 한다.
- `http_req_failed`: 실패율이 1% 미만이어야 한다.

## 입력과 출력

입력:

- 미니PC에서 실행 중인 Spring Boot 앱 인스턴스
- `/api/rooms`와 `/api/rooms/{id}` API 응답
- Spring Boot Actuator `/actuator/prometheus`
- `funchat_net` 안의 `funchat-mysqldb:3306`에서 접근 가능한 MySQL
- MySQL exporter 접속 계정

출력:

- 로컬 PC에서 `https://funchat.changee.cloud`를 대상으로 실행한 k6 콘솔 summary
- k6 remote write output을 명시해 실행한 경우 Prometheus에 저장되는 k6 지표
- Prometheus에 저장되는 Spring Boot, MySQL, Prometheus 자체 메트릭
- Grafana에서 수동 구성 후 조회 가능한 dashboard

## Jenkins 배포 연동

- Jenkins 원격 배포 스크립트는 `deploy/` 폴더와 함께 `monitoring/` 폴더를 미니PC의 `~/funchat/monitoring`으로 동기화한다.
- 앱 배포가 끝난 뒤 `docker compose --env-file <remote env file> -f monitoring/docker-compose.yml up -d prometheus grafana mysql-exporter`를 실행한다.
- 따라서 monitoring 컨테이너는 최초 배포 시 생성되고 이후 배포에서는 계속 유지된다.
- `EXPORTER_ID`, `EXPORTER_PW`는 Jenkins의 `funchat-env` file credential에 포함되어 있어야 한다.

## 알려진 제한사항

- 수평 확장 테스트용 서버 수 전환 자동화는 `monitoring` 폴더에 구현되어 있지 않다.
- k6 스크립트는 HTTP 방 목록/상세 조회만 테스트하며 WebSocket/STOMP 채팅은 테스트하지 않는다.
- 인증이 필요한 API 흐름은 테스트하지 않는다.
- Grafana datasource와 dashboard는 자동 설정되지 않는다.
- Prometheus scrape target은 배포 앱 슬롯 4개를 고정으로 수집한다. 실행하지 않은 슬롯은 `down`으로 표시된다.
- 현재 rolling 배포 스크립트의 기본 steady replica 비교 범위는 1-3개다. 4개 steady replica 운영은 rolling slot 확장 또는 직접 upstream 구성이 필요하다.
- 테스트 데이터 생성 또는 초기화 기능은 없다.
- `BASE_URL`을 지정하지 않으면 기존 로컬 직접 접근 fallback인 `http://host.docker.internal:8080`을 사용한다.
