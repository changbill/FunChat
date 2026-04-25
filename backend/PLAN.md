# FunChat Backend Plan

이 문서는 백엔드 개발 진행 상황과 다음 작업을 추적합니다. 상태 변경, 기능 범위 변경, 우선순위 변경 시 갱신합니다.

## 현재 기준

- 기준일: 2026-04-25
- 런타임: Java 21, Spring Boot 4, Gradle
- 기본 패키지: `com.funchat.demo`
- 완료 기준: 기능 구현, 테스트 작성, 테스트 통과, 관련 문서 갱신

## 진행 현황

| 영역 | 상태 | 메모 |
| --- | --- | --- |
| 프로젝트 구조 | 완료 | `auth`, `user`, `room`, `chat`, `global`, `util` 모듈 구성 |
| JWT 인증 | 완료 | 로그인, 재발급, 로그아웃, HTTP JWT 필터 구성 |
| WebSocket 인증 | 완료 | STOMP CONNECT/SUBSCRIBE/SEND 검증 흐름 구성 |
| 채팅방 API | 완료 | 생성, 목록, 상세, 수정, 삭제, 입장, 퇴장, 매니저 위임 |
| 채팅 메시징 | 완료 | `/pub/chat/message`, `/sub/chat/{roomId}` 흐름 구성 |
| 채팅 이력 | 완료 | MongoDB 기반 커서 조회 API 구성 |
| Redis 브로커 | 완료 | Streams 저장 경로와 Pub/Sub fanout 경로 구성 |
| 공통 응답 | 완료 | `ResponseDto`, `ResponseUtil` 사용 |
| 테스트 기반 | 완료 | JUnit, Testcontainers, H2 설정과 주요 서비스 테스트 존재 |
| 운영 메트릭 | 완료 | Actuator, Prometheus registry 구성 |
| 백엔드 문서 | 진행 중 | README/SPEC/PLAN 및 세부 docs 유지 필요 |

## 다음 작업

| 우선순위 | 작업 | 완료 조건 |
| --- | --- | --- |
| P0 | 현재 테스트 전체 통과 상태 확인 | `.\gradlew.bat test` 통과 |
| P0 | 인증/인가 정책 회귀 테스트 보강 | HTTP 필터, STOMP 권한 검증 핵심 케이스 테스트 |
| P1 | Controller 테스트 보강 | Auth/Room/Chat HTTP API의 성공/실패 응답 포맷 검증 |
| P1 | Room 도메인 규칙 명확화 | 정원 초과, 중복 입장, 매니저 퇴장/위임 정책 문서화 및 테스트 |
| P1 | Chat 메시지 타입 정책 정리 | `MessageType` 사용 범위와 입장/퇴장 시스템 메시지 여부 결정 |
| P2 | Mongo 조회 성능 점검 | 조회 패턴에 맞는 인덱스 필요 여부 검토 |
| P2 | Redis 장애 시 동작 정책 정리 | 저장 실패, fanout 실패, 재처리 정책 문서화 |
| P2 | API 예시 확장 | README 또는 SPEC에 실패 응답과 대표 에러 코드 추가 |

## 변경 시 체크리스트

- Controller 응답은 `ResponseUtil`과 `ResponseDto` 형식을 유지한다.
- 비즈니스 로직은 Service/Domain에 위치시킨다.
- HTTP 인증은 `Authorization: Bearer <access-token>` 정책을 유지한다.
- STOMP 인증은 native header `Authorization` 정책을 유지한다.
- Room/User는 JPA, 채팅 메시지는 MongoDB, 실시간/캐시/블랙리스트는 Redis 역할을 유지한다.
- 기능 변경 시 테스트를 추가하거나 기존 테스트를 갱신한다.
- API, DTO, 저장소, 보안 정책 변경 시 관련 문서를 함께 갱신한다.

## 문서 관리

| 문서 | 갱신 기준 |
| --- | --- |
| `README.md` | 실행 방법, 주요 API, 환경변수, 개발 진입점 변경 |
| `docs/SPEC.md` | 기능 명세, API 계약, DTO, 인증/저장소 정책 변경 |
| `docs/PLAN.md` | 진행 상태, 우선순위, 다음 작업 변경 |
| `docs/architecture.md` | 요청 흐름, 모듈 관계, 브로커 구조 변경 |
| `docs/conventions.md` | 코드 스타일, 네이밍, 계층 규칙 변경 |
| `docs/api-patterns.md` | 응답 포맷, 페이징, STOMP/API 패턴 변경 |
| `docs/db.md` | 저장소 역할, 스키마, 인덱스, 캐시 정책 변경 |
| `docs/security.md` | 인증, 인가, 토큰, 개인정보 정책 변경 |
| `docs/testing.md` | 테스트 전략, 실행 방법, 테스트 인프라 변경 |
