# Monitoring Research

작성일: 2026-04-29

## 조사 목적

단일 백엔드 서버에서 시작해 백엔드 서버 인스턴스를 수평 확장하고, Nginx 로드밸런싱을 적용했을 때 처리량, 지연 시간, 오류율, 자원 사용량이 얼마나 달라지는지 측정하기 위한 기존 `monitoring` 폴더 활용 방안을 조사한다.

이번 문서는 구현 전 조사 기록이다. 현재 구현된 기능 정의는 `SPEC.md`, 실행 계획과 진행 상태는 `PLAN.md`에 둔다.

## 현재 monitoring 구성

`monitoring/docker-compose.yml`은 미니PC 배포 서버에서 실행하는 것을 기본 전제로 다음 서비스를 정의한다.

- `prometheus`: `prom/prometheus` 이미지 사용, `monitoring/prometheus.yml`을 설정 파일로 마운트하고 `9090:9090`으로 노출한다. k6 remote write 수신을 위해 `--web.enable-remote-write-receiver` 옵션을 사용한다.
- `grafana`: `grafana/grafana` 이미지 사용, `3000:3000`으로 노출한다.
- `k6`: `grafana/k6:latest` 이미지 사용, `monitoring/scripts`를 `/scripts`로 마운트한다. 기본 실행 대상에서는 제외하고 `loadgen` profile에서만 실행한다.
- `mysql-exporter`: `prom/mysqld-exporter` 이미지 사용, `funchat_net` 내부의 `funchat-mysqldb:3306` MySQL 메트릭을 `9104`에서 노출하도록 구성되어 있다.

모든 monitoring 서비스는 외부 Docker network인 `funchat_net`에 연결된다. 이 네트워크는 `deploy/docker-compose.infra.yml`이 생성하는 배포용 네트워크다.

`monitoring/prometheus.yml`은 다음 scrape job을 가진다.

- `prometheus`: Prometheus 자체 메트릭 수집
- `spring-boot-apps`: `funchat-app-1:8081`부터 `funchat-app-4:8081`까지 앱 인스턴스별 `/actuator/prometheus` 수집
- `mysql`: `funchat-mysql-exporter:9104/metrics` 수집

`monitoring/scripts/test.js`는 k6로 방 목록 조회와 방 상세 조회를 반복하는 HTTP 부하 테스트다.

- 기본 대상: `BASE_URL` 환경변수 값. 지정하지 않으면 `http://host.docker.internal:8080`
- 주요 요청:
  - `GET /api/rooms?page={0-19}&size=20`
  - 목록 조회 성공 시 `GET /api/rooms/{roomId}`
- stages:
  - 30초 동안 50 VU까지 증가
  - 1분 동안 50 VU 유지
  - 30초 동안 1000 VU까지 증가
  - 30초 동안 0 VU로 감소
- thresholds:
  - `http_req_duration` p95 < 200ms
  - `http_req_failed` < 1%

`test.js`는 JSON parse 실패를 check 실패로 처리하고, 대상 URL을 환경변수로 바꿀 수 있다.

## 관련 배포 구성

수평 확장과 로드밸런싱 실험에는 `deploy` 폴더의 구성이 직접 관련된다.

- `deploy/docker-compose.rolling.yml`은 `app-1`부터 `app-4`까지 최대 4개의 백엔드 컨테이너 슬롯을 정의한다.
- `deploy/deploy.sh`는 rolling 배포 중 `APP_REPLICAS + 1`번 surge slot을 사용한다. 기본 `MAX_ROLLING_SLOTS=4`에서는 steady replica 1-3개가 안전하며, 4개 steady replica 비교는 rolling slot 확장 또는 직접 upstream 구성이 필요하다.
- `deploy/docker-compose.router.yml`은 `funchat-router` Nginx 컨테이너를 `80:80`으로 노출한다.
- `deploy/nginx/upstream.conf`는 `funchat_app_upstream`, `funchat_web_upstream` upstream을 정의하고 server 목록 파일을 include한다.
- `deploy/nginx/default.conf`는 `/health`, `/api/`, `/ws`를 백엔드 upstream으로 프록시한다.
- 현재 `deploy/nginx/upstream.app.servers.conf`에는 `funchat-app-1`, `funchat-app-2`, `funchat-app-3`가 포함되어 있다.

사용자 기준 서비스 품질 측정의 기본 경로는 `https://funchat.changee.cloud`다. 이 경로는 Cloudflare, 인터넷 구간, 미니PC Nginx router, 백엔드 앱, DB 영향을 모두 포함한다.

서버 자체 성능을 분리해서 볼 때는 다음 보조 경로를 사용할 수 있다.

- 로컬 단일 앱 직접 접근: `host.docker.internal:8080`
- Nginx 라우터 접근: `host.docker.internal:80` 또는 운영 도메인

사용자 체감 품질 기준의 수평 확장 효과를 보려면 모든 테스트에서 `BASE_URL=https://funchat.changee.cloud`를 고정하고, 앱 인스턴스 수만 바꾼다.

## 공식 문서 확인

Prometheus 공식 설정 문서는 설정 파일이 scrape job과 target을 정의하며, `scrape_interval`은 기본 수집 주기이고 job별로도 지정할 수 있다고 설명한다.

- 출처: https://prometheus.io/docs/operating/configuration/

k6 공식 문서는 `experimental-prometheus-rw` output을 사용하면 k6 테스트 결과 메트릭을 Prometheus remote write endpoint로 보낼 수 있다고 설명한다. 이 기능은 실험적 모듈로 표시되어 있다.

- 출처: https://grafana.com/docs/k6/latest/results-output/real-time/prometheus-remote-write/

Prometheus remote write 1.0 사양은 sender가 HTTP POST로 remote write receiver에 time series 샘플을 전송하는 프로토콜을 정의한다.

- 출처: https://prometheus.io/docs/specs/prw/remote_write_spec/

## 현재 구성으로 가능한 측정

현재 구성만으로 바로 측정 가능한 항목은 다음과 같다.

- k6 HTTP 요청 지표:
  - 요청 수
  - 요청 실패율
  - 요청 지연 시간 평균, p90, p95, p99
  - VU 수
  - checks 성공률
- Spring Boot Actuator/Prometheus 지표:
  - JVM 메모리, GC, thread
  - HTTP server request latency와 count
  - datasource connection pool 관련 지표가 노출되는 경우 DB pool 상태
- MySQL exporter 지표:
  - MySQL 연결 수
  - query 처리 관련 지표
  - InnoDB 관련 지표
- Prometheus 자체 지표:
  - scrape 성공 여부
  - scrape duration

## 현재 구성의 제약

- k6 컨테이너는 기본 profile에서 제외되어 있다. 미니PC 자원 왜곡을 줄이기 위해 로컬 PC에서 k6를 실행하는 구조를 권장한다.
- k6 결과를 Prometheus로 보내려면 `-o experimental-prometheus-rw` 옵션과 `K6_PROMETHEUS_RW_SERVER_URL=http://<MINIPC_HOST>:9090/api/v1/write` 환경변수가 필요하다.
- Prometheus에는 Grafana datasource/provisioning 설정이 없다. Grafana에서 Prometheus datasource와 dashboard를 수동으로 설정해야 한다.
- Prometheus는 앱 4개 슬롯을 모두 target으로 가진다. 실행하지 않은 슬롯은 target 화면에서 `down`으로 표시될 수 있다.
- `has content` check는 공통 응답 래퍼가 있는 경우 `body.content`, 없는 경우 `content` Page 구조를 확인한다.
- 인증이 필요한 API는 현재 테스트 대상에 포함되어 있지 않다. 현재 스크립트는 공개 방 목록/상세 조회가 가능하다는 전제에 의존한다.
- 수평 확장 시 모든 백엔드 인스턴스가 같은 DB, Redis, MongoDB를 공유하므로, 성능 향상이 앱 서버 CPU 병목에만 비례하지 않는다. DB, Redis, 네트워크, Nginx가 먼저 병목이 될 수 있다.
- WebSocket/STOMP 채팅 부하는 현재 k6 스크립트에 포함되어 있지 않다. HTTP 방 목록/상세 조회 성능과 실시간 채팅 성능은 별도로 봐야 한다.

## 비교 실험 접근 방식

### 접근 A: 기존 로컬 앱 직접 타격

`BASE_URL=http://host.docker.internal:8080`로 단일 Spring Boot 앱을 직접 부하 테스트한다.

장점:

- 현재 monitoring 구성과 가장 가깝다.
- 로드밸런서 오버헤드 없이 앱 단일 인스턴스 성능을 확인할 수 있다.

단점:

- Nginx 로드밸런싱 효과를 측정하지 못한다.
- 운영 경로와 다르다.

### 접근 B: Cloudflare와 Nginx 라우터 경유 타격

`BASE_URL=https://funchat.changee.cloud`로 k6 요청을 보낸다.

장점:

- 실제 사용자 요청 경로와 가깝다.
- Cloudflare, 인터넷 구간, 미니PC router, 앱, DB를 포함한 end-to-end 품질을 측정한다.
- upstream 서버 수를 1개, 2개, 3개로 바꿔가며 비교할 수 있다. 4개 steady replica 비교는 현재 rolling 배포 스크립트 구조상 추가 조정이 필요하다.

단점:

- Cloudflare 정책, 네트워크 변동, Nginx, 컨테이너 DNS, upstream keepalive 영향이 포함된다.
- 각 앱 인스턴스별 메트릭을 별도 target으로 수집하지 않으면 병목 분석이 어려워진다.

### 접근 C: 다중 target scrape

Prometheus에 `funchat-app-1:8081`, `funchat-app-2:8081` 같은 앱 인스턴스별 Actuator target을 추가한다.

장점:

- 로드밸런서가 요청을 실제로 분산하는지 인스턴스별 request count로 확인할 수 있다.
- 특정 인스턴스에만 지연이나 오류가 집중되는지 볼 수 있다.

단점:

- monitoring compose와 deploy compose가 같은 Docker network를 공유하거나 접근 가능한 네트워크 경로가 필요하다.
- 로컬 직접 실행과 운영 컨테이너 실행의 endpoint가 다르다.

## 실험 지표 후보

성능 향상 정도는 단일 수치 하나가 아니라 다음 지표를 함께 비교해야 한다.

- 처리량: 초당 요청 수, 완료된 iteration 수
- 지연 시간: p50, p90, p95, p99
- 안정성: HTTP 실패율, k6 check 실패율, 5xx 비율
- saturation: CPU, JVM heap, GC pause, thread 수, DB connection pool 사용률
- 병목 위치: Nginx upstream response time, 앱 인스턴스별 HTTP request count, MySQL 연결/쿼리 지표

권장 비교 방식:

- 같은 테스트 데이터와 같은 k6 시나리오를 사용한다.
- 서버 수만 1, 2, 3으로 바꾼다. 4개 비교는 rolling slot 확장 또는 직접 upstream 구성 후 별도 수행한다.
- 각 서버 수마다 warm-up 1회, 측정 3회를 실행한다.
- p95 지연 시간과 실패율이 임계값을 넘지 않는 최대 RPS 또는 최대 VU를 비교한다.

## 남은 결정 사항

- 실제 성능 실험은 미니PC에서 monitoring을 실행하고 로컬 PC에서 k6를 실행하는 구조로 진행한다.
- 사용자 기준 서비스 품질 측정의 부하 대상 URL은 `https://funchat.changee.cloud`로 고정한다.
- HTTP API만 먼저 측정할지, WebSocket/STOMP 채팅 시나리오까지 포함할지 결정해야 한다.
- Grafana dashboard를 자동 provisioning할지, 수동 구성으로 둘지 결정해야 한다.
- 부하 테스트 데이터 생성 방식과 DB 초기 상태를 고정해야 한다.
