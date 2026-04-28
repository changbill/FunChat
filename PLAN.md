# FunChat Video Streaming Plan

기준일: 2026-04-26

## 목표

디스코드 같은 소규모 그룹 화상 통화와 영상/화면 공유 시청 기능을 FunChat에 추가한다. 우선 백엔드부터 구현하고, 이후 프론트엔드에서 실제 WebRTC 참가 UI를 붙인다.

## 현재 결정 초안

- 초기 구현은 `LiveKit self-hosted SFU + 기존 Spring Boot orchestration API` 방향으로 진행한다.
- 기존 Spring Boot 백엔드를 즉시 여러 서비스로 쪼개지 않고, MSA 전환 가능한 경계를 먼저 코드와 배포에 반영한다.
- Media plane은 LiveKit이 담당하고, FunChat 백엔드는 인증/인가/방 권한/토큰 발급/세션 상태를 담당한다.
- 기존 채팅 STOMP 경로와 영상 WebRTC 경로는 분리한다.
- 방 최대 인원은 5명, 동시 영상 방 수 목표는 5개다.
- 녹화 기능은 MVP와 후속 기본 범위에서 제외한다.
- 화면 공유 기본 권장값은 1280x720, 15fps다. 글자 가독성이 중요한 화면 공유는 1920x1080, 15fps를 선택 옵션으로 둔다.
- 1차 구현은 카메라/마이크와 화면 공유까지로 제한한다.
- 운영 방식은 self-host only다.
- 영상 세션 이벤트는 LiveKit webhook으로 받는다.
- 초기 서버는 로컬 미니PC(SSD 512GB, RAM 16GB, AMD 3020e, Radeon Vega 3 Graphics)다. 이 스펙은 개발/소규모 검증용으로 보고, 5개 동시 방 목표는 부하 테스트 후 증설 또는 multi-node 검토 대상으로 둔다.
- 도메인을 구매했고 Cloudflare Tunnel로 배포 중이다. LiveKit signaling(API/WebSocket, TCP 443)은 별도 `livekit.<domain>` HTTPS 진입점으로 구성하고 Cloudflare Tunnel을 통해 미니PC LiveKit 서버로 연결한다.
- WebRTC media UDP 포트 범위는 Cloudflare Tunnel과 분리해 공유기 NAT에서 미니PC 고정 IP로 직접 포트 포워딩한다.
- `livekit.yaml`에는 `rtc.use_external_ip: true`를 설정해 STUN 기반 공인 IP 자동 감지를 활용한다. Tunnel 주소만 광고하지 않는다.
- LiveKit media UDP 포트 범위는 `50000-50100`, ICE TCP fallback은 `7881`, 내장 TURN/STUN UDP는 `3478`로 시작한다.
- 네트워크 측정값은 download 95.71 Mbit/s, upload 118.99 Mbit/s, latency 69.562 ms이며 UDP 개방 가능하다.

## MVP 범위

### 포함

- 방 참여자만 영상 세션을 시작하거나 참가할 수 있다.
- 화상 통화와 화면 공유를 같은 방 영상 세션에서 지원할 수 있도록 세션/토큰 권한 모델을 설계한다.
- 방별 영상 세션 생성/조회/종료 API를 제공한다.
- 백엔드는 LiveKit participant token을 발급한다.
- 참가자 identity, nickname, roomId를 token claim/metadata로 전달한다.
- 기본 세션 상태를 저장한다.
- LiveKit 로컬 실행 compose 구성을 추가한다.
- 백엔드 단위/서비스 테스트를 추가한다.
- 관련 README/SPEC/PLAN 문서를 갱신한다.
- 클라이언트 연계 시 adaptive stream과 dynacast를 사용할 수 있도록 백엔드/문서에 media policy를 명시한다.

### 제외

- 프론트엔드 영상 UI 구현
- 녹화/egress
- RTMP/WHIP ingest
- 대규모 방송 모드
- 별도 auth-service, room-service, chat-service 물리 분리
- 정교한 moderation UI
- 영상 파일 동시 시청 플레이어 타임라인 동기화

## 제안 서비스 경계

| 경계                | 책임                                             | 초기 구현 형태                                 |
| ------------------- | ------------------------------------------------ | ---------------------------------------------- |
| Gateway/Router      | HTTP/WebSocket 라우팅, TLS 종료                  | 기존 Nginx 확장                                |
| Core API            | Auth/User/Room 권한, 기존 Chat API               | 기존 Spring Boot 유지                          |
| Video Orchestration | 영상 세션 정책, LiveKit token 발급, webhook 처리 | Spring Boot `video` 패키지로 시작              |
| Media Plane         | WebRTC signaling/media routing, TURN 후보 제공   | LiveKit server                                 |
| Data                | 사용자/방/세션 상태, 채팅 이력                   | 기존 MySQL/Mongo/Redis + 필요 시 LiveKit Redis |

## 백엔드 API 초안

공통 응답은 기존 `ResponseDto(code, message, body)` 형식을 따른다.

| Method | Path                                                 | 인증              | 설명                                         |
| ------ | ---------------------------------------------------- | ----------------- | -------------------------------------------- |
| POST   | `/api/rooms/{roomId}/video/sessions`                 | 필요              | 방 영상 세션 시작 또는 기존 active 세션 반환 |
| GET    | `/api/rooms/{roomId}/video/session`                  | 필요              | 방의 active 영상 세션 조회                   |
| POST   | `/api/rooms/{roomId}/video/token`                    | 필요              | LiveKit join token 발급                      |
| POST   | `/api/rooms/{roomId}/video/sessions/{sessionId}/end` | 필요              | 매니저가 영상 세션 종료                      |
| POST   | `/api/video/livekit/webhook`                         | LiveKit 서명 검증 | 참가/퇴장/트랙 이벤트 수신                   |

## 데이터 모델 초안

### `VideoSession`

- `id`
- `roomId`
- `livekitRoomName`
- `status`: `ACTIVE`, `ENDED`
- `startedByUserId`
- `startedAt`
- `endedAt`

### `VideoParticipant`

- `id`
- `sessionId`
- `userId`
- `livekitIdentity`
- `joinedAt`
- `leftAt`
- `audioMuted`
- `videoMuted`
- `screenSharing`

MVP에서는 `VideoParticipant` 저장을 webhook 연동 후로 미루고, `VideoSession` + token 발급부터 시작할 수 있다.

## 구현 단계

### Phase 0: 요구사항 확정

- [x] 영상 세션 이벤트 수신 방식 확정: LiveKit webhook
- [x] UDP/TCP media port와 TLS/TURN 구성 방향 확정: 현재 로컬 배포에는 없지만 향후 개선 가능
- [x] 로컬 미니PC의 실제 네트워크 대역폭과 UDP 개방 가능 여부 확인: download 95.71 Mbit/s, upload 118.99 Mbit/s, latency 69.562 ms, UDP 개방 가능
- [x] LiveKit webhook MVP 저장 이벤트 확정: `room_started`, `room_finished`, `participant_joined`, `participant_left`
- [x] LiveKit API/WebSocket 진입점 결정: 별도 `livekit.<domain>` HTTPS 진입점, Cloudflare Tunnel 경유
- [x] WebRTC media 포트 노출 방식 결정: Cloudflare Tunnel과 별개로 공유기 NAT에서 미니PC 고정 IP로 직접 포트 포워딩
- [x] LiveKit external IP 정책 결정: `rtc.use_external_ip: true`로 STUN 기반 자동 감지 활용
- [x] 미니PC 고정 IP, NAT 포워딩, OS 방화벽 허용 규칙 검증
- [x] 공인 IP 변경 대응 방식 결정: LiveKit `rtc.use_external_ip: true` 자동 감지 활용
- [ ] 축소된 UDP 포트 범위 `50000-50100`이 실제 동시 참가자/방 규모에서 충분한지 부하 테스트
- [ ] `rtc.use_external_ip: true`가 실제 공인 IP를 올바르게 광고하는지 LiveKit 로그와 클라이언트 ICE 후보로 검증

### Phase 1: 백엔드 도메인/API 골격

- [ ] `backend/src/main/java/com/funchat/demo/video` 패키지 추가
- [ ] `VideoSession` 엔티티와 repository 추가
- [ ] `VideoSessionService`에서 Room 참여자/매니저 권한 검증 연결
- [ ] 세션 시작/조회/종료 API 추가
- [ ] 공통 예외 코드 추가
- [ ] API 단위/서비스 테스트 추가
- [ ] `backend/SPEC.md`, `backend/README.md`, `backend/PLAN.md` 갱신

### Phase 2: LiveKit 연동

- [ ] LiveKit Java server SDK 또는 REST/token 발급 방식 결정
- [ ] `LiveKitProperties` 구성 추가: API key, secret, endpoint, token TTL
- [ ] LiveKit room name 생성 정책 구현
- [ ] 방 참여자에게만 LiveKit access token 발급
- [ ] 세션 종료 시 LiveKit room 정리 연동
- [ ] SDK/API 실패 시 에러 처리와 재시도 범위 정의
- [ ] token claim 검증 가능한 테스트 추가

### Phase 3: 로컬 인프라

- [x] `deploy/docker-compose.local.yml` 또는 별도 media compose에 LiveKit 추가: `deploy/docker-compose.livekit.yml`, `deploy/livekit.yml` 단독 테스트 구성
- [ ] LiveKit Redis 사용 여부와 기존 Redis 공유 가능성 결정
- [ ] Cloudflare Tunnel에 `livekit.<domain>` -> LiveKit signaling endpoint 연결 구성 추가
- [ ] UDP `50000-50100`, TCP `7881`, UDP `3478` NAT/방화벽 규칙 문서화
- [x] LiveKit `rtc.use_external_ip: true`/NAT advertisement 설정 문서화
- [ ] `.env` 예시 변수 목록 갱신
- [ ] 로컬 미니PC 기준 부하 검증 시나리오 작성: 1-3개 방, 방당 2-5명, 화면 공유 720p/15fps

### Phase 4: 이벤트 동기화

- [ ] LiveKit webhook endpoint 추가
- [ ] webhook 서명 검증 구현
- [ ] `room_started`, `room_finished`로 영상 세션 실제 시작/종료 시간 동기화
- [ ] `participant_joined`, `participant_left`로 현재 참가자 목록 동기화
- [ ] idempotency 처리
- [ ] webhook 테스트 추가

### Phase 5: 프론트엔드 연계 준비

- [ ] 프론트가 사용할 API 계약 최종화
- [ ] LiveKit client SDK 도입 계획 수립
- [ ] 카메라 기본 품질과 화면 공유 기본 품질 적용: 화면 공유 720p/15fps, 필요 시 1080p/15fps 선택
- [ ] adaptive stream, dynacast 활성화 정책 반영
- [ ] 채팅방 화면에서 영상 패널/참가자 상태를 붙일 상태 모델 정의
- [ ] 백엔드 CORS/라우팅 정책 점검

## 검증 계획

- 백엔드 단위 테스트: 권한, 세션 상태 전이, token 발급 claim, 에러 케이스
- 컨트롤러 테스트: 인증 필요 여부, 응답 포맷, room participant 검증
- 통합 테스트: MySQL/H2 기반 `VideoSession` 저장, Room/User 관계 검증
- 로컬 수동 테스트: LiveKit 서버 실행 후 token으로 room join 가능 여부 확인
- 운영 전 점검: Nginx, SSL, UDP/TCP port, TURN 접속성, Prometheus/로그 확인

## 무중단 배포 개선 계획

기준일: 2026-04-27

기존 배포는 Docker Compose 기반 blue/green 스택과 Nginx router를 사용했다. 다만 healthcheck가 포트 오픈만 확인하고, 라우터 전환 시 Nginx 컨테이너를 재생성하며, 전환 직후 구 스택을 바로 내리기 때문에 HTTP 요청 단절 가능성이 남아 있었다.

이번 실행 범위는 `P0: 전환 중 즉시 장애를 막는 필수 작업`으로 제한한다. WebSocket graceful shutdown, 구 스택 drain, 프론트엔드 재연결, 불변 이미지 태그, DB 무중단 마이그레이션 원칙, 운영 관측 지표 고도화는 이번 작업에서 제외하고 후속 작업으로 미룬다.

### Jenkins deploy 폴더 중복 수정

기준일: 2026-04-27

문제:

- 기존 Jenkinsfile은 `scp -r deploy ${DEPLOY_USER}@${DEPLOY_HOST}:~/funchat/`로 원격 배포 파일을 덮어쓴다.
- 이 방식은 원격에 이미 생긴 `~/funchat/deploy/deploy` 중첩 폴더나 삭제된 오래된 파일을 정리하지 못한다.
- LiveKit은 단독 compose로 유지해야 하며, blue/green 앱 compose에 넣어 Jenkins 앱 배포마다 중복 기동하지 않는다.

실행 계획:

- [x] 세션 기록과 현재 `deploy` 구성에서 LiveKit 단독 운영 결정 확인
- [x] Jenkinsfile의 원격 deploy 전송 방식을 `deploy.next` 전송 후 교체 방식으로 변경
- [x] `scp -r deploy/.`를 사용해 deploy 폴더 자체가 중첩되지 않게 수정
- [x] Jenkinsfile 문법/차이 검토
- [x] 변경 내용을 세션 기록에 남김

### P0: 전환 중 즉시 장애를 막는 필수 작업

| 우선순위 | 상태 | 작업 | 대상 파일/영역 | 완료 기준 |
| --- | --- | --- | --- | --- |
| P0-1 | 완료 | Health Check를 애플리케이션 레벨로 개선한다. `/health`가 DB, Redis, MongoDB 연결과 애플리케이션 준비 상태를 확인하도록 한다. | `backend/src/main/resources/application.yml`, `SecurityConfig`, 신규 health controller/indicator, 기존 blue/green compose, `deploy/deploy.sh` | 신규 app 컨테이너가 포트 오픈만으로 healthy가 되지 않고, 실제 의존성 준비 후에만 배포 전환 대상으로 인정된다. |
| P0-2 | 완료 | Nginx 전환 방식을 컨테이너 재생성에서 reload로 바꾼다. upstream 파일 교체 후 `nginx -t`를 통과할 때만 `nginx -s reload`를 실행한다. | `deploy/deploy.sh`, `deploy/docker-compose.router.yml`, `deploy/nginx/*.conf` | router 컨테이너를 `--force-recreate`하지 않고 upstream 전환이 가능하며, 설정 오류 시 기존 upstream이 유지된다. |
| P0-3 | 완료 | 전환 후 smoke test를 추가한다. Nginx를 통해 새 upstream의 `/health`와 프론트 루트 응답을 확인한다. | `deploy/deploy.sh` | Nginx reload 후 실제 외부 경로 기준 검증이 실패하면 active color를 갱신하지 않고 롤백 절차로 넘어간다. |
| P0-4 | 완료 | 자동 롤백 전략을 추가한다. 전환 실패 또는 smoke test 실패 시 이전 upstream 파일로 복원하고 Nginx reload를 수행하며, 새 스택 로그를 남긴다. | `deploy/deploy.sh`, 기존 blue/green upstream 파일 | 실패 시 이전 blue/green 스택으로 자동 복귀하고, 구 스택은 유지된다. |

### 이번 작업에서 제외하는 후속 과제

- WebSocket graceful shutdown과 구 스택 drain 대기
- 프론트엔드 WebSocket 재연결/재구독 전략 개선
- Nginx WebSocket timeout과 장기 연결 정책 고도화
- Docker 이미지 불변 태그 적용
- DB 무중단 마이그레이션 원칙 문서화와 적용
- 배포 로그/관측 지표 고도화

### 무중단 배포 검증 시나리오

- 배포 전: active color, app replica 수, `/health` 상태, Nginx config test 결과를 확인한다.
- 신규 스택 기동 후: 모든 app replica가 애플리케이션 health를 통과해야 한다.
- 전환 직후: Nginx 경유 `/health`와 정적 프론트 루트 응답을 확인한다. 인증 API와 WebSocket/STOMP 검증은 후속 작업에서 다룬다.
- 실패 시: 이전 upstream으로 복원하고 Nginx reload 후 새 스택 로그를 수집한다.
- 성공 시: 이번 범위에서는 기존 방식대로 구 스택을 종료한다. drain 대기는 후속 작업에서 다룬다.
- 회귀 검증: 배포 중 REST 요청 반복, 로그인 상태 유지, 프론트 정적 리소스 응답을 확인한다. WebSocket 무중단 보장은 후속 작업에서 별도 검증한다.

### 남은 위험

- 이번 P0 범위만으로는 WebSocket 장기 연결 무중단을 보장하지 않는다.
- DB 스키마 변경이 포함된 배포는 이번 범위에서 무중단을 보장하지 않는다.
- Cloudflare Tunnel, 공유기 NAT, 미니PC 네트워크 장애는 애플리케이션 배포 전략의 범위 밖이다. 외부 진입점 장애 대응은 별도 운영 계획이 필요하다.

## 롤링 배포 전환 계획

기준일: 2026-04-28

배경:

- 운영 대상이 클라우드 로드밸런서/오토스케일링 환경이 아니라 단일 미니PC다.
- blue/green은 새 스택 전체를 동시에 띄워야 해서 미니PC 자원 사용량이 커진다.
- 현재 목표는 Nginx router와 infra는 유지하고 backend/frontend 앱 슬롯만 한 개씩 교체하는 것이다.

실행 계획:

- [x] 기존 blue/green compose, Nginx upstream, Jenkins 호출 방식 확인
- [x] `deploy/docker-compose.rolling.yml` 추가: 상시 `app-1..3`, `web-1..3`와 surge `app-4`, `web-4` 고정 슬롯 정의
- [x] `deploy/deploy.sh`를 롤링 슬롯 교체 방식으로 변경
- [x] Nginx upstream 블록은 `deploy/nginx/upstream.conf`로 분리하고, 스크립트는 server 목록 파일만 갱신하도록 변경
- [x] backend/frontend 배포 구간 시작 시 surge 슬롯을 1회 생성하고 정상 확인 후 upstream에 투입
- [x] surge 슬롯을 유지한 상태에서 기존 슬롯을 하나씩 upstream에서 제외하고 Nginx reload 수행
- [x] backend 슬롯은 `/health` healthy 확인 후 재투입
- [x] frontend 슬롯은 컨테이너 running 확인 후 재투입
- [x] 각 슬롯 교체 후 Nginx 경유 smoke test 수행
- [x] Jenkins의 기존 `APP_REPLICAS` 환경변수는 롤링 슬롯 수로 유지
- [x] 불필요해진 blue/green compose와 upstream 파일 삭제
- [x] Jenkins 원격 env 파일을 `mktemp`/`umask 077` 기반 임시 파일로 만들고, Jenkins/deploy 양쪽에서 삭제 시도
- [x] Docker Hub 비밀번호를 SSH 명령줄 인자 대신 원격 임시 credential 파일로 전달
- [x] 배포 중 실패해도 원격 `docker logout`을 cleanup trap에서 시도하도록 보강
- [x] Jenkins Docker push 절차를 `deploy/scripts/docker-push-images.sh`로 분리
- [x] Jenkins 원격 배포 절차를 `deploy/scripts/jenkins-remote-deploy.sh`로 분리
- [x] `deploy.sh` 공통 credential/cleanup/http 함수를 `deploy/scripts/deploy-common.sh`로 분리
- [x] README/RESEARCH/PLAN 문서 갱신

검증 결과와 남은 검증:

- [ ] `bash -n deploy/deploy.sh`로 스크립트 문법 검증: 현재 Windows/WSL, Git Bash 실행 권한 문제로 로컬 검증 실패. 미니PC 또는 Jenkins 실행 환경에서 재검증 필요
- [x] `docker compose -f deploy/docker-compose.rolling.yml config`로 compose 문법 검증
- [x] `docker compose -f deploy/docker-compose.router.yml config`로 router compose 문법 검증
- [x] `docker compose -f deploy/docker-compose.infra.yml config`로 infra compose 문법 검증
- [ ] 원격 미니PC에서 `APP_REPLICAS=3` 기준 최초 배포와 재배포를 각각 확인
- [ ] 배포 중 `/health`, `/`, 로그인 API, STOMP 연결 재연결 동작을 수동 확인

남은 위험:

- 배포 중에는 backend 또는 frontend 컨테이너가 일시적으로 `APP_REPLICAS + 1`개까지 늘어나므로 미니PC의 CPU/RAM 여유가 필요하다.
- `latest` 이미지 태그를 계속 쓰면 실패 슬롯을 이전 이미지로 자동 되돌리는 강한 rollback은 어렵다.
- Docker compose 환경변수는 컨테이너 메타데이터에 남으므로, Docker 소켓 접근 권한이 있는 사용자는 secret 값을 볼 수 있다. 운영 서버의 docker 그룹 권한을 제한해야 한다.
- WebSocket 장기 연결은 교체 대상 슬롯에서 끊길 수 있다. 클라이언트 재연결과 Spring Boot graceful shutdown은 후속 작업으로 다룬다.
- 현재 rolling compose는 상시 최대 3개 슬롯과 surge 1개를 정의한다. 더 많은 슬롯이 필요하면 compose 서비스 정의, `MAX_APP_REPLICAS`, `MAX_ROLLING_SLOTS`를 함께 늘려야 한다.

## 위험과 대응

| 위험                                                        | 영향                                   | 대응                                                                                     |
| ----------------------------------------------------------- | -------------------------------------- | ---------------------------------------------------------------------------------------- |
| WebRTC 배포 포트/방화벽 누락                                | 클라이언트 접속 실패                   | UDP `50000-50100`, TCP `7881`, UDP `3478` NAT/방화벽 규칙 검증                           |
| 기존 Room 참여 상태와 LiveKit participant 상태 불일치       | 권한 누수 또는 유령 참가자             | token TTL 짧게 설정, webhook idempotency 처리                                            |
| LiveKit 장애 시 세션 상태 불일치                            | UI/권한 혼란                           | 세션 조회 시 LiveKit 상태 확인 또는 reconciliation job 추가                              |
| 기존 단일 Spring Boot가 계속 커짐                           | MSA 전환 비용 증가                     | `video` 패키지를 별도 service boundary처럼 설계                                          |
| 화면 공유/동시 시청 요구 혼재                               | MVP 지연                               | 세션/토큰 기반은 공통으로 먼저 구현하고, 영상 파일 동기화는 별도 단계로 분리             |
| 5명 x 5개 방에서 고해상도 화면 공유 남용                    | LiveKit 대역폭/CPU 비용 증가           | 기본 720p/15fps, adaptive stream, dynacast, 1080p 옵션 제한                              |
| 로컬 미니PC 단일 서버 성능 한계                             | 5개 동시 방 목표 미달                  | 로컬은 소규모 검증용으로 제한하고, 부하 테스트 후 LiveKit multi-node 또는 서버 증설 검토 |
| Cloudflare Tunnel만으로 WebRTC media path를 처리한다고 오해 | 영상 연결 실패 또는 TURN fallback 의존 | signaling은 `livekit.<domain>`/Tunnel, media는 NAT 포워딩과 external IP 광고로 분리      |
| 공인 IP 변경                                                | 외부 클라이언트 ICE 후보 불일치        | `rtc.use_external_ip: true` 자동 감지를 사용하고 LiveKit 로그/ICE 후보로 검증            |
| UDP 포트 범위를 50000-50100으로 좁게 설정                   | 동시 참가자 증가 시 포트 부족          | MVP 부하 테스트 후 필요하면 공식 기본 범위에 가까운 더 넓은 범위로 확장                  |

## 다음 액션

1. Phase 0 요구사항을 확정한다.
2. LiveKit Java 연동 방식을 조사해 Phase 2 세부 구현안을 확정한다.
3. 백엔드 `video` 패키지와 `VideoSession` API부터 작은 PR 단위로 구현한다.
4. 롤링 배포는 미니PC에서 최초 배포와 재배포를 모두 검증하고, 이후 WebSocket graceful shutdown과 불변 이미지 태그를 후속 작업으로 진행한다.
