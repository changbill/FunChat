# Testing

## 테스트 스택

- **JUnit 5**: 기본 테스트 프레임워크
- **Spring Boot Test**: 통합 테스트 지원
- **Testcontainers**: Redis/Mongo 등 외부 의존성을 컨테이너로 구동
- **H2**: JPA 관련 테스트에서 인메모리 DB로 사용(`application-test.yml`)

## 실행 방법

Windows(PowerShell) 기준 예시:

```bash
cd backend
.\gradlew.bat test
```

또는(환경에 따라):

```bash
cd backend
./gradlew test
```

## Testcontainers 규칙

- Redis, Mongo는 `TestContainerTest`(추상 베이스)에서 컨테이너로 띄우고
  `@DynamicPropertySource`로 스프링 프로퍼티를 오버라이드합니다.
- 통합 테스트가 Redis/Mongo에 의존한다면, 해당 베이스를 상속하는 방식으로 작성합니다.

## 테스트 작성 가이드

- **Service 테스트**는 “비즈니스 규칙 검증”에 집중하고, I/O 의존은 최소화합니다.
- **WebSocket/브로커 테스트**는 레이어가 복잡하므로:
  - 프로토콜/인증/권한(StompHandler) 규칙이 바뀌지 않게 회귀 테스트를 유지
  - Mongo 저장 경로/팬아웃 경로는 각각 단위/통합을 분리해 검증

## 레이어별 테스트 전략

- **Domain 레이어**: 순수 단위 테스트 (Spring 컨텍스트, JPA 의존 없이 순수 Java로 검증)
- **Service 레이어**: `@ExtendWith(MockitoExtension.class)` 사용
- **Controller 레이어**: `@WebMvcTest` 사용
- **Repository 레이어**: `@DataJpaTest` 사용
