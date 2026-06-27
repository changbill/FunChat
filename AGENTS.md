## 1. 기본 원칙

* 프로젝트는 기본적으로 `README.md`, `RESEARCH.md`, `PLAN.md`, `SPEC.md`를 기준으로 관리한다.
* 문서 간 내용은 중복하지 않는다.
* 구현 전에는 조사, 계획, 명세를 구분해서 정리한다.
* 구현 중에는 기존 코드 스타일, 로컬 헬퍼, 타입 규칙, 테스트 구조를 따른다.
* 테스트, 타입 체크, 린트, 빌드 등 검증 작업은 계획에 포함한다.
* 기록은 형용사와 부사를 줄이고 사실 중심으로 작성한다.

---

## 2. 브랜치 규칙

* 작업 브랜치는 항상 `feature/...` 형식을 사용한다.
* 현재 브랜치가 `main` 또는 `master`이면 구현 시작 전에 새 브랜치를 생성하고 전환한다.
* 브랜치 이름은 `feature/{번호}-{phase제목}` 형식을 사용한다.

예시:

```bash
feature/2-auth-login
feature/3-payment-api
```

---

## 3. 문서 역할

| 파일                                          | 역할                                                                        |
| ------------------------------------------- | ------------------------------------------------------------------------- |
| `README.md`                                 | 프로젝트 입문 문서. 목적, 주요 기능, 실행 방법, 개발 환경, 디렉터리 구조, 운영 주의사항을 기록한다.              |
| `RESEARCH.md`                               | 구현 전 조사 문서. 공식 문서, 참조 구현, 라이브러리 비교, API 제약, 대안 비교, 선택 근거를 기록한다.           |
| `PLAN.md`                                   | 앞으로 할 일 문서. 구현 범위, 단계별 todo, 테스트 계획, 검증 방법, 결정 필요 사항을 기록한다.               |
| `SPEC.md`                                   | 현재 구현 명세 문서. 실제 구현된 기능의 동작, 입출력, 상태 전이, 권한, 에러 처리, 외부 연동, 알려진 제한사항을 기록한다. |
| `.codex/markdown/{phase}/implementation.md` | 완료한 PLAN 단계의 구현 요약을 기록한다.                                                 |
| `.codex/markdown/{phase}/verification.md`   | 완료한 PLAN 단계의 검증 결과를 기록한다.                                                 |

---

## 4. 문서 작성 기준

### `README.md`

* 담당 개발자가 아니어도 프로젝트를 이해할 수 있도록 작성한다.
* 프로젝트 목적, 주요 기능, 실행 방법, 개발 환경, 디렉터리 구조, 운영 주의사항을 기록한다.
* 프로젝트 이해에 필요한 정보가 바뀌면 갱신한다.

### `RESEARCH.md`

* 구현 전에 조사한 근거를 기록한다.
* 공식 문서, 인터넷 검색, 참조 구현, 라이브러리 비교, API 제약, 시스템 제약, 중요한 발견 사항을 기록한다.
* 여러 접근 방식의 장단점을 비교한다.
* 선택하거나 배제한 이유를 남긴다.

### `PLAN.md`

* 앞으로 진행할 작업만 기록한다.
* 단계별 todo와 체크리스트를 관리한다.
* 테스트 계획과 검증 방법을 포함한다.
* 사용자의 인라인 주석과 피드백이 있으면 반영한다.
* 완료된 작업의 상세 구현 내용은 `implementation.md`로 옮긴다.

### `SPEC.md`

* 현재 코드 기준으로 실제 구현된 기능만 기록한다.
* 사용자 관점의 기능 동작을 기록한다.
* 입력, 출력, 상태 전이, 권한, 에러 처리, 외부 연동, 알려진 제한사항을 기록한다.
* 기능 정의가 바뀌면 갱신한다.

---

## 5. Phase 기준

* Phase는 `PLAN.md`의 큰 단계를 기준으로 한다.
* 예를 들어 `PLAN.md`의 `2단계` 산출물은 `.codex/markdown/phase-2/`에 기록한다.
* Phase 내부 todo가 모두 완료되면 구현 요약과 검증 결과를 작성한다.
* Phase 내부에 더 진행할 todo가 없으면 merge 내용을 요약하고 PR을 생성한다.

산출물 경로 예시:

```text
.codex/markdown/phase-2/implementation.md
.codex/markdown/phase-2/verification.md
```

---

## 6. 구현 기록

* 각 Phase가 끝날 때 `.codex/markdown/{phase}/implementation.md`를 작성하거나 갱신한다.
* 구현 기록에는 완료한 작업, 변경 파일, 주요 설계 판단, 영향 범위를 기록한다.
* 구현된 기능 정의가 바뀌면 `SPEC.md`를 갱신한다.
* 프로젝트 이해에 필요한 정보가 바뀌면 `README.md`를 갱신한다.

작성 예시:

```markdown
# Implementation

- `application-test.yml` test profile H2 datasource 설정
- `FakeS3TestConfig` S3 호출 대체
- `AuthServiceTest` 로그인 성공 케이스 추가
```

---

## 7. 검증 기록

* 검증은 단계 마지막에 모아서 수행한다.
* 가능한 범위에서 타입 체크, 테스트, 린트, 빌드를 실행한다.
* 검증 결과는 `.codex/markdown/{phase}/verification.md`에 기록한다.
* 실패가 있으면 실패 원인, 수정 내용, 재검증 결과를 기록한다.
* 실행하지 못한 검증이 있으면 사유를 남긴다.

작성 예시:

```markdown
# Verification

- `./gradlew test` 통과
- `./gradlew build` 통과
- `npm run lint` 통과
```

실패 기록 예시:

```markdown
# Verification

- `./gradlew test` 실패
  - 원인: `S3Client` 실제 빈 주입
  - 수정: `FakeS3TestConfig` 추가
  - 재검증: `./gradlew test` 통과
```

---

## 8. 사용자 피드백 반영

* 사용자의 피드백이 있으면 관련 산출물을 수정한다.
* 기능 정의가 바뀌면 `SPEC.md`를 갱신한다.
* 계획이 바뀌면 `PLAN.md`를 갱신한다.
* 조사 근거가 추가되면 `RESEARCH.md`를 갱신한다.
* 구현 결과가 바뀌면 `.codex/markdown/{phase}/implementation.md`를 갱신한다.
* 검증 결과가 바뀌면 `.codex/markdown/{phase}/verification.md`를 갱신한다.

---

## 9. PR 생성 기준

* Phase 내부 todo가 모두 완료되면 PR을 생성한다.
* PR에는 merge 내용을 요약한다.
* PR에는 구현 요약과 검증 결과를 포함한다.
* 미완료 항목, 제한사항, 후속 작업이 있으면 명시한다.

PR 요약 형식:

```markdown
## Summary

- 로그인 API 구현
- JWT 발급 로직 추가
- 인증 실패 에러 응답 정리

## Verification

- `./gradlew test` 통과
- `./gradlew build` 통과

## Notes

- 소셜 로그인 연동은 다음 Phase에서 진행
```
