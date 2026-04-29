# Monitoring Plan

작성일: 2026-04-29

## 작업 목표

현재 `monitoring` 폴더의 Prometheus, Grafana, k6 구성을 활용해 단일 서버와 수평 확장 서버 구성을 비교하는 성능 테스트 체계를 만든다.

이번 계획의 초점은 "서버 수를 늘리고 Nginx 로드밸런싱을 적용하면 성능이 얼마나 좋아지는가"를 반복 측정 가능한 방식으로 확인하는 것이다.

## 현재 상태

- `monitoring/docker-compose.yml`에 Prometheus, Grafana, loadgen profile의 k6, mysql-exporter가 정의되어 있다.
- `monitoring/prometheus.yml`은 Prometheus 자체, 배포 앱 슬롯 4개, MySQL exporter를 scrape한다.
- `monitoring/scripts/test.js`는 방 목록 조회와 상세 조회 HTTP API를 대상으로 k6 부하를 생성하며, `BASE_URL` 환경변수로 대상 URL을 바꿀 수 있다.
- `deploy` 폴더에는 Nginx router와 최대 4개 백엔드 앱 슬롯이 있다.
- 성능 테스트 기본 구조는 미니PC에서 Prometheus/Grafana/exporter를 실행하고 로컬 PC에서 k6로 미니PC를 원격 타격하는 방식이다.

## 구현 범위

이번 테스트 체계에서 다룰 범위:

- 실제 사용자 접속 경로 `https://funchat.changee.cloud` 기준 단일 앱 측정
- 실제 사용자 접속 경로 `https://funchat.changee.cloud` 기준 2개, 3개 앱 인스턴스 측정
- 4개 앱 인스턴스 측정은 rolling slot 확장 또는 surge slot 없는 직접 upstream 구성 방식을 확정한 뒤 진행
- k6 결과를 Prometheus에 저장
- Spring Boot Actuator, MySQL exporter, Prometheus 지표를 함께 확인
- 측정 결과를 Grafana 또는 Prometheus query로 비교

이번 범위에서 제외하거나 후순위로 둘 항목:

- WebSocket/STOMP 채팅 부하 테스트
- LiveKit 음성/영상 부하 테스트
- 자동 오토스케일링
- 클라우드 로드밸런서 비교
- DB 샤딩 또는 DB 수평 확장

## 수정 예정 파일

- `monitoring/scripts/test.js`
  - [x] `BASE_URL`을 환경변수로 받을 수 있게 변경
  - [ ] stage를 smoke, baseline, stress 등 목적별로 분리할 수 있게 검토
  - [x] JSON parse 실패와 응답 구조 차이를 안전하게 처리
- `monitoring/prometheus.yml`
  - [x] 다중 앱 인스턴스별 scrape target 추가
  - [ ] 로컬/배포 환경별 target 분리 방식 검토
- `monitoring/docker-compose.yml`
  - [x] 미니PC 배포 네트워크 `funchat_net` 연결
  - [x] k6를 `loadgen` profile로 분리
  - [x] Jenkins 배포 후 monitoring compose 자동 기동
  - Grafana datasource provisioning 추가 검토
- `monitoring/README.md`
  - [x] 실제 실행 명령, 결과 확인 방법, 비교 절차 정리
- `monitoring/SPEC.md`
  - [x] 구현 후 실제 동작으로 갱신

## 테스트 매트릭스

| 케이스 | 요청 경로 | 앱 인스턴스 수 | 목적 |
| --- | --- | ---: | --- |
| A | `https://funchat.changee.cloud` | 1 | 사용자 경로 기준 단일 앱 품질 |
| B | `https://funchat.changee.cloud` | 2 | 사용자 경로 기준 2개 앱 분산 효과 |
| C | `https://funchat.changee.cloud` | 3 | 사용자 경로 기준 3개 앱 분산 효과 |
| D | `https://funchat.changee.cloud` | 4 | rolling slot 확장 또는 직접 upstream 구성 후 추가 비교 |

각 케이스는 같은 DB 데이터, 같은 k6 시나리오, 같은 테스트 시간으로 반복한다.

이 매트릭스는 Cloudflare 경유 end-to-end 품질을 측정한다. 서버 자체 성능 분리가 필요하면 별도 보조 테스트로 미니PC IP 직접 접근 또는 앱 포트 직접 접근을 추가한다.

## 측정 기준

핵심 비교 지표:

- k6 `http_req_duration` p95, p99
- k6 `http_req_failed`
- k6 초당 요청 수
- k6 iteration rate
- Spring Boot HTTP request count와 latency
- JVM heap, GC, thread 지표
- MySQL connection과 query 관련 지표
- Nginx access log의 upstream 분산 여부

성공 기준 초안:

- 실패율 1% 미만을 유지한다.
- p95 지연 시간이 200ms 이하인 구간의 최대 VU 또는 최대 RPS를 비교한다.
- 서버 수 증가에 따라 처리량이 증가하는지 확인하되, 선형 증가를 가정하지 않는다.
- p95/p99가 급증하는 지점과 DB/JVM/CPU 병목 지표를 함께 기록한다.

## 단계별 계획

### 1단계: 현재 구성 검증

- [x] `monitoring` 폴더 파일 목록 확인
- [x] Prometheus, Grafana, k6, mysql-exporter 구성 확인
- [x] 기존 k6 시나리오 확인
- [x] `deploy`의 Nginx upstream과 앱 슬롯 구성 확인
- [x] 미니PC monitoring + 로컬 k6 구조로 설정 변경
- [x] Jenkins 배포 시 monitoring 폴더 동기화와 Prometheus/Grafana/MySQL exporter `up -d` 추가
- [ ] 현재 구성 그대로 Prometheus/Grafana/k6 실행 가능 여부 확인
- [ ] `https://funchat.changee.cloud` 기준 단일 앱 측정

### 2단계: k6 시나리오 정리

- [x] `BASE_URL`을 환경변수화
- [x] 부하 단계 주석과 실제 VU 수 불일치 수정
- [ ] smoke, baseline, stress 실행 모드 설계
- [x] 응답 JSON parse 실패 방어
- [x] 테스트 결과를 Prometheus remote write로 보내는 실행 명령 문서화

### 3단계: Prometheus 수집 대상 정리

- [ ] Cloudflare 경유 테스트와 서버 내부 scrape 지표를 함께 해석하는 기준 정리
- [ ] Nginx router 경유 테스트 시 필요한 target 정리
- [x] 다중 앱 인스턴스별 Actuator scrape target 추가 가능성 확인
- [ ] MySQL exporter 접속 계정과 권한 확인
- [ ] Grafana datasource provisioning 여부 결정

### 4단계: 수평 확장 비교 실험

- [ ] 앱 1개 구성에서 `https://funchat.changee.cloud` 테스트 실행
- [ ] 앱 2개 구성에서 `https://funchat.changee.cloud` 테스트 실행
- [ ] 앱 3개 구성에서 `https://funchat.changee.cloud` 테스트 실행
- [ ] 앱 4개 구성 방식 확정 후 `https://funchat.changee.cloud` 테스트 실행
- [ ] 각 구성별 k6 summary와 Prometheus 지표 저장
- [ ] upstream별 request count 또는 Nginx 로그로 분산 여부 확인

### 5단계: 결과 해석과 문서화

- [ ] 단일 앱 대비 2, 3개 앱의 처리량 향상률 계산
- [ ] 4개 앱 구성 테스트가 수행되면 별도 향상률 계산
- [ ] p95/p99 지연 시간 변화 정리
- [ ] 실패율 변화 정리
- [ ] 병목 후보를 JVM, DB, Redis, Nginx, 네트워크로 분류
- [ ] `monitoring/SPEC.md`를 실제 구현 기준으로 갱신
- [ ] `monitoring/README.md`에 실행 방법과 해석 방법 작성

## 검증 방법

구성 검증:

- `docker compose -f monitoring/docker-compose.yml config`
- Prometheus target 화면에서 scrape 상태 확인
- Grafana에서 Prometheus datasource 연결 확인
- k6 smoke test로 API 응답과 check 성공률 확인

부하 테스트 검증:

- 같은 테스트 데이터를 사용해 반복 실행
- 각 케이스를 최소 3회 측정하고 중앙값 또는 대표값 사용
- 테스트 직후 Prometheus query와 k6 summary의 주요 수치 비교
- 실패율이 높으면 결과를 성능 향상으로 해석하지 않고 병목/오류 분석 대상으로 분리

## 위험과 대응

| 위험 | 영향 | 대응 |
| --- | --- | --- |
| DB가 먼저 병목이 됨 | 앱 서버 증설 효과가 작게 보임 | DB connection, query 지표를 함께 보고 병목으로 분리 |
| k6 실행 머신이 먼저 한계에 도달 | 서버 성능이 아니라 부하 생성기 한계를 측정 | k6 CPU 사용량 확인, 필요 시 외부 부하 생성기 사용 |
| 테스트 데이터가 부족함 | 캐시/빈 응답 때문에 실제 부하와 달라짐 | 방/메시지 seed 데이터 기준을 고정 |
| 인증/권한 조건이 실제 사용자 흐름과 다름 | 공개 API 성능만 측정됨 | HTTP 공개 API 측정 후 로그인 포함 시나리오 추가 |
| WebSocket 부하가 빠짐 | 채팅 서비스 전체 성능으로 일반화하기 어려움 | HTTP API 결과와 실시간 채팅 결과를 분리 |
| 다중 앱 메트릭 수집 불가 | 병목 인스턴스 식별 어려움 | monitoring과 deploy 네트워크 연결 또는 host port 노출 검토 |

## 현재 진행 상태

2026-04-29:

- `monitoring` 폴더의 현재 Prometheus/Grafana/k6/MySQL exporter 구성을 조사했다.
- `deploy` 폴더의 Nginx router와 rolling 앱 슬롯 구성을 확인했다.
- 구현 전 조사 내용은 `RESEARCH.md`, 실행 계획은 이 문서, 현재 기능 정의는 `SPEC.md`로 분리했다.
- 미니PC에서 Prometheus/Grafana/MySQL exporter를 실행하고 로컬 PC에서 k6를 실행하는 구조로 설정을 변경했다.
