# FunChat Video Streaming Research

기준일: 2026-04-26

## 배경

FunChat은 현재 JWT 인증, 채팅방, STOMP 채팅, Redis Streams/PubSub, MongoDB 이력 저장을 갖춘 Spring Boot 기반 채팅 서비스다. 새 목표는 디스코드처럼 소규모 그룹이 같은 방에서 영상/음성을 공유하고 함께 보는 기능을 MSA 방식으로 확장하는 것이다.

이번 조사 범위는 구현 전 아키텍처 선택과 백엔드 우선 개발 계획 수립이다. 현재 기능 정의는 `backend/SPEC.md`를 기준으로 하고, 이 문서는 아직 구현되지 않은 영상 스트리밍 설계 검토만 기록한다.

## 현재 코드베이스 발견 사항

- 현재 백엔드는 단일 Spring Boot 애플리케이션이며 패키지 기준으로 `auth`, `user`, `room`, `chat`, `global`, `util`로 나뉜다.
- HTTP API는 `ResponseDto(code, message, body)` 형식을 사용하고, 인증은 `Authorization: Bearer <access-token>` JWT 정책을 사용한다.
- 실시간 채팅은 SockJS/STOMP `/ws`, publish `/pub/chat/message`, subscribe `/sub/chat/{roomId}` 구조다.
- 방 참여 여부 검증은 STOMP `SUBSCRIBE`/`SEND` 단계에서 수행된다.
- MySQL은 User/Room, MongoDB는 채팅 메시지, Redis는 Pub/Sub, Streams, 캐시, 토큰 블랙리스트에 사용된다.
- 배포는 Docker Compose, Nginx router, blue/green backend/web, 별도 infra compose 구조다.
- 현재 Nginx는 HTTP API와 `/ws` WebSocket 프록시만 고려하고 있으며 WebRTC용 UDP/TCP media port, TURN, SFU 라우팅은 없다.
- Jenkins 배포 단계는 `scp -r deploy ...:~/funchat/`로 원격 `deploy` 폴더를 반복 전송한다. 이 방식은 과거 잘못 전송된 `~/funchat/deploy/deploy` 중첩 폴더를 정리하지 못하고, 삭제된 compose/nginx 파일도 원격에 잔존시킬 수 있다.

## 외부 자료 요약

### WebRTC 기본 제약

- WebRTC는 브라우저 네이티브 실시간 오디오/비디오 전송 기술이며, 일반 WebSocket보다 미디어 전송과 네트워크 변화 대응에 적합하다.
- 브라우저 간 연결에는 ICE가 필요하고, NAT/firewall 환경에서 STUN/TURN 서버가 필요하다.
- WebRTC 자체는 signaling 프로토콜을 정하지 않으므로 방 참여, SDP/ICE 교환, 권한 확인 같은 signaling API는 애플리케이션이 설계해야 한다.

참고:

- MDN WebRTC protocols: https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Protocols
- WebRTC TURN server guide: https://webrtc.org/getting-started/turn-server

### P2P, SFU, MCU 비교

- P2P는 2-3명 이하 소규모에는 단순하지만, 참여자가 늘수록 송신자가 여러 업로드 스트림을 유지해야 해서 확장성이 급격히 나빠진다.
- MCU는 서버에서 영상을 디코드/믹싱/인코딩하므로 클라이언트 수신은 단순하지만 서버 CPU 비용과 지연이 커진다.
- SFU는 서버가 미디어 트랙을 선택적으로 중계하고, 각 클라이언트는 필요한 트랙을 구독한다. 그룹 영상 채팅과 화면 공유에는 일반적으로 SFU가 적합하다.

참고:

- LiveKit SFU internals: https://docs.livekit.io/reference/internals/livekit-sfu/
- mediasoup overview: https://mediasoup.org/documentation/overview/

### LiveKit 검토

- LiveKit은 WebRTC SFU, signaling, SDK, self-hosting, recording/egress, ingress 등을 제공한다.
- Self-hosted LiveKit은 데이터/인프라 제어권을 유지할 수 있고, 로컬/VM/Kubernetes/multi-region 배포 옵션을 제공한다.
- 분산 LiveKit 구성에서는 Redis가 필요하며, room 참가자는 같은 media node로 라우팅되는 구조를 제공한다.
- 운영 시 SSL, WebSocket endpoint, UDP/TCP ICE port, TURN/TLS, 방화벽 설정이 핵심이다.
- LiveKit은 화면 공유를 video track으로 발행하며, h720(1280x720), h540, h360, h180 같은 video quality preset과 simulcast/dynacast/adaptive stream 기능을 제공한다. 5명 방 5개 동시 목표에서는 adaptive stream과 dynacast를 전제로 클라이언트가 실제 표시하는 track만 고품질로 받게 해야 한다.

참고:

- LiveKit about: https://docs.livekit.io/intro/about/
- LiveKit self-hosting: https://docs.livekit.io/transport/self-hosting/
- LiveKit deployment: https://docs.livekit.io/home/self-hosting/deployment/
- LiveKit ports and firewall: https://docs.livekit.io/home/self-hosting/ports-firewall/

### mediasoup 검토

- mediasoup은 Node.js 모듈 형태의 낮은 수준 SFU다.
- signaling agnostic 설계라 signaling, room model, 인증, 배포, 관측, SDK 통합을 직접 구현해야 한다.
- 세밀한 제어가 필요한 경우 유리하지만, 현재 FunChat의 우선 목표인 백엔드 우선 MVP에는 구현 범위와 운영 리스크가 크다.

참고:

- mediasoup overview: https://mediasoup.org/documentation/overview/

### TURN 서버

- 직접 연결이 어려운 네트워크에서는 TURN relay가 필요하다.
- coturn은 오픈소스 STUN/TURN 서버 선택지다.
- LiveKit은 embedded TURN 서버를 제공하며, TURN/TLS 또는 TURN/UDP 설정을 통해 접속성을 보완할 수 있다.

참고:

- WebRTC TURN server guide: https://webrtc.org/getting-started/turn-server
- coturn GitHub: https://github.com/coturn/coturn
- LiveKit deployment TURN section: https://docs.livekit.io/home/self-hosting/deployment/

## 선택지

### 선택지 A: 기존 Spring Boot에 signaling만 직접 구현하고 P2P WebRTC 사용

장점:

- 미디어 서버 없이 시작 가능하다.
- 기존 STOMP room 인증 흐름을 재사용할 수 있다.

단점:

- 3명 이상 또는 화면 공유 + 다중 수신에서 업로드 비용이 커진다.
- NAT/TURN, 재접속, track 상태, 품질 제어를 직접 다뤄야 한다.
- 이후 SFU로 전환할 때 프론트/백엔드 signaling 계약이 크게 바뀔 수 있다.

판단:

- 디스코드형 소규모 그룹이라도 3명 이상과 화면 공유를 고려하면 장기 선택으로 부적합하다.

### 선택지 B: mediasoup 기반 media-service 직접 구축

장점:

- 미디어 레이어 제어권이 높다.
- signaling, 권한, room 정책을 FunChat 도메인에 맞게 완전히 설계할 수 있다.

단점:

- Node.js media-service, signaling API, worker lifecycle, codec/transport 설정, metrics, TURN 연동을 직접 구현해야 한다.
- 백엔드 MVP까지 시간이 길고 운영 리스크가 크다.

판단:

- WebRTC/SFU 자체를 학습하거나 세밀한 커스텀 요구가 확정된 경우 적합하다. 현재는 과한 선택이다.

### 선택지 C: LiveKit self-hosted SFU + Spring Boot orchestration service

장점:

- SFU, signaling, SDK, room token, reconnect, adaptive streaming, TURN/egress 등 많은 운영 기능을 검증된 구현으로 시작할 수 있다.
- 기존 Spring Boot는 인증, 방 권한, 세션 정책, 토큰 발급, 감사 로그에 집중할 수 있다.
- MSA 경계가 명확하다. `core-api`는 도메인 권한을 관리하고, `media-service/LiveKit`은 미디어 plane을 담당한다.

단점:

- LiveKit 서버와 API key/secret 관리가 추가된다.
- 배포에 UDP/TCP port, SSL, TURN, Redis 설정이 추가된다.
- LiveKit의 room/participant 모델과 FunChat Room 모델의 동기화 정책을 설계해야 한다.

판단:

- 현재 목표에는 이 선택지가 가장 현실적이다. 초기 MVP는 LiveKit self-hosted 단일 노드 또는 cloud-compatible abstraction으로 시작하고, Spring Boot에서 `video-session` API와 LiveKit access token 발급을 담당한다.

## 권장 아키텍처 방향

초기 백엔드 MVP는 기존 Spring Boot를 바로 쪼개기보다, 다음 두 경계로 시작한다.

1. Core API
   - 기존 FunChat Spring Boot.
   - 사용자, 채팅방, 방 참여 권한, JWT 인증, 채팅 유지.
   - 영상 세션 생성/조회/종료 API와 LiveKit token 발급 담당.

2. Media Plane
   - LiveKit server self-hosted.
   - WebRTC signaling/media routing, participant/track 관리.
   - 필요 시 Redis, TURN, egress service 추가.

이후 MSA 전환 단계에서 `room-service`, `chat-service`, `auth-service`, `video-orchestration-service`, `gateway`로 분리한다. 단, 첫 구현은 분산 트랜잭션과 서비스 간 인증 비용을 피하기 위해 modular monolith + external media service 형태가 안전하다.

## 주요 설계 결정 초안

- 영상 방은 기존 `Room`에 종속된다. 별도 공개 영상방을 만들지 않는다.
- `Room` 참여자만 영상 세션 join token을 발급받을 수 있다.
- LiveKit room name은 내부 식별자로 `funchat-room-{roomId}` 형태를 사용한다.
- 클라이언트는 FunChat JWT를 LiveKit에 직접 전달하지 않는다. 백엔드가 짧은 만료 시간의 LiveKit participant token을 발급한다.
- participant identity는 FunChat userId 기반 고정 문자열을 사용하고, nickname은 token metadata/name으로 전달한다.
- 영상 세션 상태는 MySQL에 최소 상태만 저장한다. 실제 media participant/track 상태는 LiveKit API 또는 webhook/event로 동기화한다.
- 채팅 STOMP와 영상 WebRTC는 분리한다. 채팅 메시지 경로를 영상 signaling으로 재사용하지 않는다.
- MVP 범위는 camera/mic join/leave, screen share, participant list, mute state, session lifecycle이다.
- 녹화, VOD, 동시 시청 동기화 플레이어, 대규모 방송, moderation 고도화는 후속 단계로 둔다.
- 녹화/egress는 현재 범위에서 제외한다.

## 확정된 요구사항

- 영상 기능 목표는 화상 통화와 한 명이 영상 파일/화면을 공유해 같이 보는 기능을 모두 포함한다.
- 방 최대 인원은 5명이다.
- 동시 영상 방 수 목표는 5개다.
- 녹화 기능은 필요 없다.
- 화면 공유 기본 권장값은 1280x720, 15fps로 둔다. 문서/코드 공유처럼 작은 글자가 중요한 화면은 옵션으로 1920x1080, 15fps를 허용하되, 기본값으로는 사용하지 않는다.
- 1차 구현 범위는 카메라/마이크와 화면 공유다. 영상 파일 같이 보기와 타임라인 동기화는 후속 단계로 분리한다.
- 운영 방식은 self-host only로 한다.
- 영상 세션 이벤트는 LiveKit webhook으로 수신한다.
- 현재 로컬 배포에는 UDP/TCP media port와 TLS/TURN 구성이 없지만, 향후 개선 가능성을 전제로 설계한다.
- 도메인을 구매했고 Cloudflare Tunnel을 통해 배포 중이다. LiveKit signaling(API/WebSocket, TCP 443)은 별도 `livekit.<domain>` HTTPS 진입점으로 구성하고, 이 진입점은 Cloudflare Tunnel을 통해 미니PC의 LiveKit 서버로 연결한다.
- WebRTC media path는 Cloudflare Tunnel과 분리한다. 공유기 NAT에서 LiveKit media UDP 포트 범위를 미니PC의 고정 IP로 직접 포트 포워딩하고, 인터넷에 직접 노출한다.
- LiveKit 설정 파일(`livekit.yaml`)에는 `rtc.use_external_ip: true`를 설정해 STUN 기반 공인 IP 자동 감지를 활용한다. Tunnel 주소만 광고하면 클라이언트가 미디어 데이터를 보낼 실제 ICE 후보를 찾지 못해 연결이 끊길 수 있다.
- LiveKit media UDP 포트 범위는 `50000-50100`으로 시작한다. 공식 기본값 `50000-60000`보다 좁은 범위지만, 로컬 미니PC MVP 규모를 기준으로 방화벽/NAT 운영을 단순화하기 위한 초기값이다.
- ICE TCP fallback은 `rtc.tcp_port: 7881`로 사용한다.
- 내장 TURN/STUN을 사용할 경우 `turn.udp_port: 3478`을 사용한다.
- 네트워크 측정값은 download 95.71 Mbit/s, upload 118.99 Mbit/s, latency 69.562 ms(Allied Telesis Capital Corp., Okinawa 기준)다.
- UDP 개방은 가능하다.

## Jenkins deploy 폴더 동기화 조사

요청 세션에서 확인한 운영 판단은 LiveKit을 blue/green 앱 compose에 중복 포함하지 않고, 별도 `docker-compose.livekit.yml`로 단독 운영하는 방향이다. LiveKit은 `network_mode: host`, `7880/tcp`, `7881/tcp`, `50000-50100/udp`처럼 호스트 포트를 직접 사용하므로 blue와 green 양쪽에 동시에 넣으면 같은 포트를 점유하려고 충돌할 수 있다.

Jenkinsfile의 기존 전송 방식은 로컬 `deploy` 디렉터리를 원격 `~/funchat/` 아래로 디렉터리째 복사한다. 배포 대상에 기존 `deploy`가 남아 있거나, 이전 실패/수동 복사로 `deploy/deploy`가 생긴 경우 새 배포가 이를 제거하지 않는다. 따라서 배포 산출물은 `~/funchat/deploy.next`에 먼저 전송하고, 전송 성공 후 `~/funchat/deploy`로 교체하는 방식이 더 명확하다.

선택한 해결책:

- `scp -r deploy/.`로 deploy 폴더 자체가 아니라 내부 파일만 `deploy.next`에 복사한다.
- 원격에서 기존 `deploy`는 `deploy.prev`로 이동하고, `deploy.next`를 현재 `deploy`로 교체한다.
- Jenkins는 여전히 앱 blue/green 배포만 실행하며, LiveKit 단독 compose는 자동 기동하지 않는다.

## 서버 스펙 메모

초기 self-host 대상 서버는 로컬 미니PC이며 스펙은 SSD 512GB, RAM 16GB, AMD 3020e, Radeon Vega 3 Graphics다.

이 서버는 개발/소규모 검증용으로 다루는 것이 안전하다. LiveKit SFU는 미디어를 서버에서 인코딩하지 않는 구조라 MCU보다 가볍지만, 5명 방 5개 동시 목표는 네트워크 대역폭, 패킷 처리량, TURN relay 비율, 화면 공유 비율에 크게 좌우된다. 따라서 초기 로컬 검증 목표와 장기 동시 방 목표를 분리한다.

- 로컬 1차 검증: 1-3개 방, 방당 2-5명, 화면 공유 720p/15fps
- 부하 검증 전제: 현재 측정된 download 95.71 Mbit/s, upload 118.99 Mbit/s 기준으로 UDP 안정성, TURN relay 비율, 실제 동시 송수신 대역폭을 측정
- 운영 목표 5개 동시 방: 단일 미니PC 확정 스펙이 아니라, 부하 테스트 후 서버 증설 또는 LiveKit multi-node 구성을 검토해야 하는 목표치로 둔다.

## LiveKit Webhook MVP 이벤트 범위

MVP에서는 다음 이벤트를 저장한다.

- `room_started`, `room_finished`: 영상 세션의 실제 시작/종료 시간을 기록하고, 비정상 종료된 방을 정리할 때 사용한다.
- `participant_joined`, `participant_left`: 현재 영상 방에 누가 있는지 실시간 목록을 DB에 동기화한다. STOMP 연결 끊김이나 클라이언트 상태 누락을 보완하는 기준으로 사용한다.

## 확정된 LiveKit 네트워크 설정 초안

```yaml
rtc:
  port_range_start: 50000
  port_range_end: 50100
  tcp_port: 7881
  use_external_ip: true

turn:
  enabled: true
  udp_port: 3478
```

공유기 NAT와 OS 방화벽에서 허용할 포트는 다음과 같다.

| 목적 | 포트 | 프로토콜 | 설정 |
| --- | --- | --- | --- |
| Signaling/API/WebSocket | 443 | TCP | Cloudflare Tunnel -> `livekit.<domain>` |
| ICE UDP media | 50000-50100 | UDP | 미니PC 고정 IP로 직접 포트 포워딩 |
| ICE TCP fallback | 7881 | TCP | `rtc.tcp_port` |
| TURN/STUN | 3478 | UDP | `turn.udp_port`, 내장 TURN/STUN 사용 시 |

## 남은 확인 사항

- 축소된 UDP 포트 범위 `50000-50100`이 실제 동시 참가자/방 규모에서 충분한지 부하 테스트로 검증해야 한다.
- `rtc.use_external_ip: true`가 현재 회선/공유기 환경에서 올바른 공인 IP를 광고하는지 LiveKit 로그와 클라이언트 ICE 후보로 확인해야 한다.
