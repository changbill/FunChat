# Conventions

## 디렉터리/패키지 구조

- 소스 루트: `src/main/java/com/funchat/demo`
- 테스트 루트: `src/test/java/com/funchat/demo`

권장 패키지 레이어링(현재 코드 기준):

- `**/controller`: HTTP API 엔드포인트
- `**/service`: 비즈니스 로직(트랜잭션, 도메인 조합)
- `**/domain`: 엔티티/리포지토리 및 도메인 모델
- `**/domain/dto`: 요청/응답 DTO(HTTP/WS 등)
- `global`: config/filter/exception/constants/aop/annotation 등 공통
- `util`: 범용 유틸(`ResponseUtil`, `ParseUtil` 등)

## 계층 규칙

- **Controller**
  - 입력 검증(`@Valid`)과 인증 주입(`@AuthenticationPrincipal`)까지만 담당
  - 도메인 로직/영속 로직을 직접 수행하지 않음(반드시 Service 호출)
  - 반환은 `ResponseUtil`을 통해 `ResponseDto`로 통일
- **Service**
  - 비즈니스 규칙의 중심
  - 트랜잭션 경계는 Service에서 관리하는 것을 기본으로 함
- **Domain**
  - JPA 엔티티는 도메인 상태/행위를 포함할 수 있으나, 외부 리소스 접근은 Service에 둠
  - Repository는 `domain` 하위에 위치

## 기본 스타일

- 인코딩: UTF-8, 줄바꿈: LF, 들여쓰기: 4 spaces
- 파일명: PascalCase + 용도 (`UserController`, `MealSuggestionService`)
- import는 와일드카드 비사용, 알파벳 정렬
- 최대한 `final` 선언 활용
- FQCN 직접 사용 금지, `import`로 클래스명 명시

## 핵심 컨벤션

- 도메인 설계: 엔티티/VO는 불변성·캡슐화 우선, 공통 시간 필드는 `BaseTimeEntity` 사용
- Repository: 인터페이스를 도메인에 두고, `JpaRepository`/구현체를 인프라로 분리 (3층 구조)
- DTO: record 우선, Request/Response 분리, setter 지양
- 메서드는 30줄 이내, 단계별 private 메서드로 책임 분리
- 반복 문자열/매직 넘버는 상수로 추출
- 단, 에러 메시지는 상수 추출 금지

## Lombok 규칙

- 엔티티: `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 서비스/레포지토리/컴포넌트: `@RequiredArgsConstructor` (생성자 주입 통일)

## 네이밍

- **컨벤션**
  - 클래스/인터페이스: `PascalCase`
  - 메서드/변수: `camelCase`
  - 상수: `UPPER_SNAKE_CASE`
  - DTO: `...Request`, `...Response`
  - 조회: `find`/`get`, 쓰기: `create`/`update`/`delete`
  - Boolean 판별: `is`/`has`/`can` 접두어

- **엔드포인트**
  - REST: `/api/...` 아래로 구성
  - 인증: `/api/auth/**`
  - 방: `/api/rooms/**`
  - WebSocket endpoint: `/ws`
  - STOMP subscribe: `/sub/...`, publish: `/pub/...`

## 예외 처리/에러 코드

- 도메인/서비스에서 비즈니스 예외는 `BusinessException` + `ErrorCode` 사용을 기본으로 함
- Controller는 가능한 한 예외를 “잡아서 변환”하지 않고(중복), 공통 예외 처리 흐름을 따름

## 응답 포맷

- 성공: `ResponseUtil.createSuccessResponse(body)` 또는 오버로드
- 실패: `ResponseUtil.createErrorResponse(errorCode, body)` 패턴 유지

응답 DTO는 `ResponseDto(Integer code, String message, Object body)` 형태를 사용합니다.

## 보안/인증 데이터 전달

- HTTP: `Authorization: Bearer <access-token>`
- STOMP: native header `Authorization: Bearer <access-token>`
- STOMP SEND 시 roomId는 native header `roomId`를 사용(코드와 호환 유지)
