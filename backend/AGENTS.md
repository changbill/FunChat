# Backend AGENTS

프로젝트/패키지/아키텍처 설명은 `backend/README.md`와 `docs/DOCUMENTATION.md`를 참고한다.

## 1. 작업 시작 규칙

- 작업 시작 전 `backend/README.md`, `backend/RESEARCH.md`, `backend/PLAN.md`, `backend/SPEC.md`를 확인한다.
- 공통 규칙은 최상위 `AGENTS.md`를 따른다.

## 2. 코드 변경 규칙(백엔드)

- 계층은 `Controller → Service → Domain(Entity/Repository)` 흐름을 유지한다.
- Controller는 요청/응답만 담당하고 비즈니스 로직은 Service에 둔다.
- 트랜잭션은 Service 계층에서 관리한다. 조회는 `readOnly`를 우선한다.
- 응답은 `ResponseUtil`로 `ResponseDto(code, message, body)` 포맷을 유지한다.
- HTTP 인증 헤더는 `Authorization: Bearer <access-token>`을 유지한다.
- STOMP 인증은 native header `Authorization` 흐름을 유지한다.
- `@RestController`와 `@RequestMapping("/api/...")`를 사용한다.
- 입력 DTO는 `@RequestBody`로 받고, 필요한 경우 `@Valid`를 적용한다.
- 인증 사용자 정보는 `@AuthenticationPrincipal`로 받는다.
- Controller에서 컬렉션 필터링, 정렬, 권한 판단 같은 비즈니스 로직을 구현하지 않는다.
- 예외는 도메인 예외와 `GlobalExceptionHandler`로 분리한다.
- 민감 파라미터는 로그에서 제외한다.

### 기본 스타일

- 인코딩은 UTF-8, 줄바꿈은 LF, 들여쓰기는 4 spaces를 사용한다.
- 파일명은 PascalCase + 용도(`UserController`, `RoomService`)로 작성한다.
- 와일드카드 import와 FQCN 직접 사용을 피한다.
- 가능한 곳에는 `final`을 사용한다.
- DTO는 record를 우선 사용하고 Request/Response를 분리한다.
- 반복 문자열과 매직 넘버는 상수로 추출하되, 에러 메시지는 상수화하지 않는다.

### Lombok 및 네이밍

- 엔티티는 `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 기본으로 한다.
- 서비스/리포지토리/컴포넌트는 `@RequiredArgsConstructor`로 생성자 주입을 통일한다.
- 클래스/인터페이스는 `PascalCase`, 메서드/변수는 `camelCase`, 상수는 `UPPER_SNAKE_CASE`를 사용한다.
- DTO 이름은 `...Request`, `...Response`로 끝낸다.
- 조회 메서드는 `find`/`get`, 쓰기 메서드는 `create`/`update`/`delete`를 사용한다.
- Boolean 판별 메서드는 `is`/`has`/`can` 접두어를 사용한다.

## 3. 저장소/외부 연동 규칙(백엔드)

- User/Room 등 코어 데이터는 JPA+MySQL 흐름을 유지한다.
- 채팅 메시지 영속은 MongoDB 흐름을 유지한다.
- 실시간/팬아웃/캐시/블랙리스트는 Redis 역할을 유지한다.
- 미디어 전송(signaling/media)은 LiveKit이 담당하고, 백엔드는 세션 상태/토큰 발급을 담당한다.
- 저장소/연동 방식 변경이 필요하면 근거를 Phase `research.md`에 먼저 기록한다.
- 엔티티는 `room/domain`, `user/domain` 등 도메인 패키지 `domain` 하위에 둔다.
- JPA 연관관계와 `@Table`, `@Column` 제약(길이, nullable, unique, index, FK)을 명확히 설정한다.
- enum은 `@Enumerated(EnumType.STRING)`을 기본으로 사용하고, DB 상태 값은 대문자 enum 문자열로 통일한다.
- MongoDB 조회 패턴을 변경할 때는 인덱스, 조회 로직, DTO를 함께 검토한다.
- JPA 테스트는 H2 in-memory DB를 사용한다.
- Redis/Mongo 의존 통합 테스트는 Testcontainers를 사용한다. 상세는 `testing.md`를 따른다.

## 4. 테스트/검증 규칙(백엔드)

- 기능 구현/수정 시 테스트를 추가하거나 갱신한다.
- 엔드포인트, DTO, 저장소 스키마, 인증 정책 변경 시 `README.md`, `SPEC.md`, `PLAN.md`, `testing.md` 중 관련 문서를 함께 갱신한다.
- 가능하면 아래 검증을 수행하고 결과를 Phase `verification.md`에 기록한다.

```bash
./gradlew test
./gradlew build
```

## 5. 문서 갱신 규칙(백엔드)

- Phase 조사 진행 중에는 `.codex/markdown/backend/phase-{번호}/research.md`에 기록한다.
- Phase 완료 시 `research.md` 내용을 요약해 `backend/RESEARCH.md`에 정리한다.
- 기능 동작/계약이 바뀌면 `backend/SPEC.md`를 갱신한다.
- 실행 방법/환경/진입점이 바뀌면 `backend/README.md`를 갱신한다.
