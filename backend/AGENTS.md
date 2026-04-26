# Funchat 프로젝트 Backend Agent Guide

## 참조 문서

| 문서         | 내용                                                                                       |
| ------------ | ------------------------------------------------------------------------------------------ |
| `README.md`  | 서비스 개요, 시스템 아키텍처, 스키마, 개발 규칙, 백엔드 컨벤션, 코딩 스타일, API 설계 규칙 |
| `SPEC.md`    | 도메인 모델, API 엔드포인트, 비즈니스 규칙 상세                                            |
| `PLAN.md`    | Phase별 개발 계획 및 체크리스트                                                            |
| `testing.md` | 테스트 전략, Testcontainers, 실행 방법                                                     |

## 빠른 구조 요약

- **언어/프레임워크**: Java 21, Spring Boot(Gradle)
- **주요 기술**: Spring MVC, Spring Security(JWT), WebSocket(STOMP/SockJS), Redis, MongoDB, JPA(MySQL)
- **기본 패키지 루트**: `com.funchat.demo`

대략적인 모듈 구분(패키지):

- `auth`: JWT, `UserDetails` 등 인증/인가
- `user`: 회원/로그인/토큰 재발급/로그아웃 API
- `room`: 채팅방 CRUD/입장/퇴장/매니저 위임 등
- `chat`: WebSocket(STOMP) 인바운드 처리, Redis 브로커, Mongo 저장, HTTP 메시지 조회 API
- `global`: 공통 설정(config), 필터(filter), 예외(exception), 상수(constants), AOP 등
- `util`: 공통 유틸(응답 포맷, 파싱 등)

## 반드시 지켜야 하는 규칙

- **테스트 작성**: 기능 구현 또는 수정 시 반드시 테스트를 작성하고 통과시킨 후 완료로 간주한다.
- **응답 포맷 고정**: 컨트롤러는 `ResponseUtil`로 `ResponseDto(code, message, body)` 반환을 유지합니다.
- **계층 분리**: Controller → Service → Domain(엔티티/리포지토리) 흐름을 유지합니다. (비즈니스 로직은 Controller에 두지 않기)
- **인증**:
  - HTTP: `Authorization: Bearer <access-token>` (필터에서 검증)
  - WebSocket(STOMP): native header `Authorization`로 토큰 전달(인바운드 채널 인터셉터에서 검증)
- **DB 사용처**:
  - Room/User 등 코어 데이터: JPA(MySQL)
  - 채팅 메시지 영속: MongoDB
  - 실시간/팬아웃/블랙리스트/캐시: Redis
- **문서 우선**: 설계/컨벤션/패턴/DB/테스트는 참조 문서를 기준으로 작업합니다.

## 문서화 규칙

- 새 기능/테이블/엔드포인트 변경 시 `README.md`를 갱신한다.
- 기능 명세 변경 시 `SPEC.md`를, 진행 상황 변경 시 `PLAN.md`를 갱신한다.
- 테스트 추가 또는 변경 시 `testing.md`를 갱신한다.
- 개인정보 처리는 기본 비공개 정책을 선행한다.

## 우선순위 가이드

1. `README.md`와 충돌 시 이 문서의 운영 지침을 우선 적용한다.
2. 현재 위치의 설정/의존성은 변경 전후로 확인 후 반영한다.
