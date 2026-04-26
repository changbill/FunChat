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

## 테스트 규칙

- given, when, then 순으로 작성한다.
- 경계값 분석, 오류 추정 기법을 기본으로 사용한다.
- Mocking을 사용할 경우는 외부 API를 사용할 경우에 한한다.

## Testcontainers 규칙

- Redis, Mongo는 `TestContainerTest`(추상 베이스)에서 컨테이너로 띄우고
  `@DynamicPropertySource`로 스프링 프로퍼티를 오버라이드합니다.
- 통합 테스트가 Redis/Mongo에 의존한다면, 해당 베이스를 상속하는 방식으로 작성합니다.

## 테스트 작성 가이드

- 테스트 이름은 시나리오 중심으로 작성 (`when_..._then_...`)
- 테스트 대상 public 메서드마다 `@Nested` 클래스로 묶고, `@DisplayName`에는 해당 메서드의 사용자 관점 동작을 적는다.
- `@Nested` 내부의 테스트 메서드는 성공/실패/경계 조건을 분리해서 작성한다.
- **Controller 테스트**는 웹 레이어를 검증한다.
- **Service 테스트**는 “비즈니스 규칙 검증”에 집중한다.
- **Repository 테스트**는 DB 계층을 슬라이스 테스트한다.
- **WebSocket/브로커 테스트**는 레이어가 복잡하므로:
  - 프로토콜/인증/권한(StompHandler) 규칙이 바뀌지 않게 회귀 테스트를 유지
  - Mongo 저장 경로/팬아웃 경로는 각각 단위/통합을 분리해 검증

예시:

```java
@Nested
@DisplayName("몽고디비에 메시지 저장")
class SaveMessage {

    @Test
    @DisplayName("성공")
    void success() {
        chatService.saveMessageToMongo(messageMap);
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }
}
```

## 레이어별 테스트 전략

- **Domain 레이어**: 순수 단위 테스트 (Spring 컨텍스트, JPA 의존 없이 순수 Java로 검증)
- **Service 레이어**: `@SpringBootTest` + `@Transactional` 사용 (Mock 사용 금지)
- **Controller 레이어**: `@WebMvcTest` 사용
- **Repository 레이어**: `@DataJpaTest` 사용, JPA를 사용하지 않을 경우 `TestContainer`와 `@DynamicPropertySource` 사용
