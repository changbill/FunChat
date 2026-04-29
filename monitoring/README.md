# Monitoring

Funchat 배포 서버의 Prometheus/Grafana 모니터링과 k6 부하 테스트 구성을 둔다.

이 문서의 기본 목표는 실제 사용자 접속 경로인 `https://funchat.changee.cloud` 기준으로 앱 인스턴스 수를 1개에서 3개까지 바꿨을 때 서비스 품질이 어떻게 달라지는지 측정하는 것이다. 4개 앱 비교는 rolling slot 확장 또는 직접 upstream 구성을 확정한 뒤 추가로 수행한다.

## 실행 구조

권장 구조:

- 미니PC: Spring Boot 앱, frontend, Nginx router, MySQL, Redis, MongoDB 실행
- 미니PC: Prometheus, Grafana, MySQL exporter 실행
- 로컬 PC: k6 실행
- k6 대상 URL: `https://funchat.changee.cloud`
- k6 결과 저장 위치: 미니PC Prometheus remote write endpoint

이 구조는 서버 자원과 부하 생성기 자원을 분리한다. k6를 미니PC에서 직접 실행하면 부하 생성기가 서버 CPU와 네트워크를 함께 사용하므로, 서버 성능 측정값이 왜곡될 수 있다.

요청 흐름:

```text
로컬 PC k6
  -> Cloudflare
  -> https://funchat.changee.cloud
  -> 미니PC 80번 포트
  -> funchat-router Nginx
  -> funchat-app-1..4
  -> MySQL/Redis/MongoDB
```

## 사전 조건

미니PC:

- Docker와 Docker Compose plugin이 설치되어 있어야 한다.
- `deploy/.env` 또는 배포 스크립트가 사용하는 env 파일이 준비되어 있어야 한다.
- `funchat.changee.cloud`가 미니PC의 Nginx router로 연결되어 있어야 한다.
- 미니PC에서 `9090` Prometheus 포트에 로컬 PC가 접근할 수 있어야 한다.
- MySQL exporter 계정이 MySQL에 있어야 한다.
- Jenkins 배포를 사용할 경우 `funchat-env` file credential에 `EXPORTER_ID`, `EXPORTER_PW`가 포함되어 있어야 한다.

로컬 PC:

- k6 CLI를 설치하거나 Docker로 k6를 실행할 수 있어야 한다.
- `https://funchat.changee.cloud`에 접근할 수 있어야 한다.
- `http://<MINIPC_HOST>:9090/api/v1/write`에 접근할 수 있어야 k6 결과를 Prometheus에 저장할 수 있다.

## 1. 미니PC에서 서비스 실행

명령은 repository root에서 실행한다.

먼저 infra와 배포 앱/router가 떠 있어야 한다. 일반 배포는 `deploy/deploy.sh`가 담당한다. 수동으로 최소 구성만 확인할 때는 다음 순서로 실행할 수 있다.

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.infra.yml up -d
docker compose --env-file deploy/.env -p funchat-rolling -f deploy/docker-compose.rolling.yml up -d app-1 web-1
docker compose --env-file deploy/.env -f deploy/docker-compose.router.yml up -d
```

운영 배포 스크립트를 사용할 때는 `APP_REPLICAS` 값으로 정상 상태의 앱 개수를 정한다.

```bash
APP_REPLICAS=1 ENV_FILE=deploy/.env ./deploy/deploy.sh
```

앱 개수별 비교 테스트를 할 때는 같은 방식으로 `APP_REPLICAS`를 바꿔 배포한다.

```bash
APP_REPLICAS=2 ENV_FILE=deploy/.env ./deploy/deploy.sh
APP_REPLICAS=3 ENV_FILE=deploy/.env ./deploy/deploy.sh
```

주의: 현재 `deploy/deploy.sh`는 rolling 배포 중 `APP_REPLICAS + 1`번 surge slot을 사용한다. `MAX_ROLLING_SLOTS` 기본값이 `4`라서 스크립트 방식의 steady replica 비교는 기본적으로 1-3개가 안전하다. 4개 앱까지 steady 상태로 비교하려면 surge slot 없이 4개 upstream을 직접 구성하거나, rolling slot 수를 5개 이상으로 확장하는 별도 변경이 필요하다.

## 2. 미니PC에서 모니터링 실행

`monitoring/docker-compose.yml`은 외부 Docker network `funchat_net`에 붙는다. 이 네트워크는 `deploy/docker-compose.infra.yml` 실행으로 만들어진다.

Jenkins 배포를 사용하면 `deploy/scripts/jenkins-remote-deploy.sh`가 `monitoring/` 폴더를 미니PC의 `~/funchat/monitoring`으로 동기화하고, 배포가 끝난 뒤 다음 compose 명령을 실행한다. 따라서 최초 배포 때 monitoring 컨테이너가 없으면 생성되고, 이후 배포에서는 계속 유지된다.

```bash
docker compose --env-file monitoring/.env -f monitoring/docker-compose.yml up -d prometheus grafana mysql-exporter
```

Jenkins가 아닌 미니PC shell에서 직접 실행할 때는 repository root에서 위 명령을 사용한다. Jenkins 배포에서는 별도 수동 실행이 필요하지 않다.

상태 확인:

```bash
docker compose --env-file monitoring/.env -f monitoring/docker-compose.yml ps
docker logs --tail=100 funchat-prometheus
docker logs --tail=100 funchat-mysql-exporter
```

브라우저 확인 URL:

- Prometheus: `http://<MINIPC_HOST>:9090`
- Prometheus targets: `http://<MINIPC_HOST>:9090/targets`
- Grafana: `http://<MINIPC_HOST>:3000`

Prometheus targets에서 확인할 것:

- `prometheus` target이 `UP`
- `mysql` target이 `UP`
- 실행 중인 `funchat-app-N:8081` target이 `UP`
- 실행하지 않은 앱 슬롯은 `DOWN`이어도 정상

예를 들어 앱 1개만 테스트 중이면 `funchat-app-2`, `funchat-app-3`, `funchat-app-4`는 `DOWN`으로 보일 수 있다.

## 3. 로컬 PC에서 k6 실행

로컬 PC에서 실행한다. repository root가 로컬 PC에도 있어야 `monitoring/scripts/test.js` 경로를 그대로 사용할 수 있다.

PowerShell에서 k6 CLI로 실행:

```powershell
$env:BASE_URL='https://funchat.changee.cloud'
$env:K6_PROMETHEUS_RW_SERVER_URL='http://<MINIPC_HOST>:9090/api/v1/write'
k6 run -o experimental-prometheus-rw monitoring/scripts/test.js
```

Docker로 k6 실행:

```powershell
docker run --rm `
  -e BASE_URL=https://funchat.changee.cloud `
  -e K6_PROMETHEUS_RW_SERVER_URL=http://<MINIPC_HOST>:9090/api/v1/write `
  -v ${PWD}/monitoring/scripts:/scripts `
  grafana/k6:latest run -o experimental-prometheus-rw /scripts/test.js
```

`<MINIPC_HOST>`는 로컬 PC에서 미니PC에 접근 가능한 IP 또는 호스트명으로 바꾼다. 예를 들어 미니PC LAN IP가 `192.168.0.10`이면 다음처럼 쓴다.

```powershell
$env:K6_PROMETHEUS_RW_SERVER_URL='http://192.168.0.10:9090/api/v1/write'
```

## 4. 앱 인스턴스 수별 비교 절차

모든 테스트에서 `BASE_URL`은 `https://funchat.changee.cloud`로 고정한다. 앱 개수만 바꿔야 비교가 가능하다.

권장 반복 절차:

1. `APP_REPLICAS=1`로 배포한다.
2. Prometheus targets에서 `funchat-app-1`만 `UP`인지 확인한다.
3. 로컬 PC에서 k6를 1회 warm-up으로 실행한다.
4. 같은 조건으로 k6를 3회 측정 실행한다.
5. k6 summary와 Prometheus 지표를 기록한다.
6. `APP_REPLICAS=2`, `3`에 대해 같은 절차를 반복한다.
7. 4개 앱 비교가 필요하면 rolling slot 확장 또는 4개 upstream 직접 구성 방식을 먼저 확정한다.

기록할 값:

- 앱 인스턴스 수
- 테스트 시작/종료 시각
- k6 `http_req_duration` p95, p99
- k6 `http_req_failed`
- k6 `http_reqs`
- k6 `iterations`
- checks 성공률
- 앱 인스턴스별 HTTP request count
- JVM heap, GC, thread 지표
- MySQL connection/query 관련 지표

성능 향상률은 실패율 1% 미만과 p95 지연 시간 기준을 만족하는 최대 처리량을 기준으로 비교한다. 평균 응답 시간만으로 판단하지 않는다.

## 5. Prometheus에서 확인할 지표

k6 결과가 remote write로 들어오면 Prometheus에서 k6 메트릭을 조회할 수 있다. 메트릭 이름은 k6 output 버전에 따라 달라질 수 있으므로 Prometheus UI의 autocomplete로 `k6_` prefix를 먼저 확인한다.

Spring Boot Actuator 지표 예시:

```promql
rate(http_server_requests_seconds_count[1m])
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))
jvm_memory_used_bytes
jvm_threads_live_threads
```

MySQL exporter 지표는 exporter 버전에 따라 이름이 다를 수 있다. Prometheus UI에서 `mysql_` prefix로 확인한다.

인스턴스별 요청 분산은 `instance` label을 기준으로 본다.

```promql
sum by (instance) (rate(http_server_requests_seconds_count[1m]))
```

## 6. Grafana 기본 연결

Grafana datasource provisioning은 아직 자동화되어 있지 않다. 최초 1회 수동으로 설정한다.

1. `http://<MINIPC_HOST>:3000` 접속
2. 기본 계정으로 로그인
3. Prometheus datasource 추가
4. URL에 `http://prometheus:9090` 입력
5. Save & Test

Grafana 컨테이너는 Prometheus와 같은 Docker network에 있으므로 datasource URL은 외부 IP가 아니라 `http://prometheus:9090`을 사용한다.

## 7. 종료

모니터링만 내릴 때:

```bash
docker compose --env-file monitoring/.env -f monitoring/docker-compose.yml down
```

배포 서비스까지 내릴 때는 데이터 볼륨 삭제 여부를 주의한다.

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.router.yml down
docker compose --env-file deploy/.env -p funchat-rolling -f deploy/docker-compose.rolling.yml down
docker compose --env-file deploy/.env -f deploy/docker-compose.infra.yml down
```

DB 데이터를 지우려면 volume까지 삭제해야 하지만, 성능 비교 테스트에서는 데이터 조건을 유지해야 하므로 일반적으로 volume은 지우지 않는다.

## 문제 해결

Prometheus에서 app target이 `DOWN`:

- 해당 앱 슬롯 컨테이너가 실행 중인지 확인한다.
- 앱이 `8081` 포트로 실행 중인지 확인한다.
- monitoring compose가 `funchat_net`에 연결되어 있는지 확인한다.

MySQL exporter가 `DOWN`:

- `monitoring/.env`의 `EXPORTER_ID`, `EXPORTER_PW`가 MySQL 계정과 맞는지 확인한다.
- MySQL 컨테이너 이름이 `funchat-mysqldb`인지 확인한다.
- exporter 계정에 필요한 권한이 있는지 확인한다.

k6 결과가 Prometheus에 안 들어옴:

- 로컬 PC에서 `http://<MINIPC_HOST>:9090/api/v1/write`에 접근 가능한지 확인한다.
- k6 실행 명령에 `-o experimental-prometheus-rw`가 있는지 확인한다.
- `K6_PROMETHEUS_RW_SERVER_URL` 값이 미니PC Prometheus를 가리키는지 확인한다.

Cloudflare 경유 테스트가 403, 429, 5xx를 반환:

- Cloudflare WAF, bot protection, rate limiting 정책을 확인한다.
- 이 경우 결과는 서버 성능 한계가 아니라 Cloudflare 정책 영향을 포함한다.
- 사용자 체감 품질에는 의미가 있지만, 서버 자체 병목 분석과는 분리해서 기록한다.
