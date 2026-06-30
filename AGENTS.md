## 1. 기본 원칙

- 프로젝트는 기본적으로 `README.md`, `RESEARCH.md`, `PLAN.md`, `SPEC.md`를 기준으로 관리한다.
- 문서 간 내용은 중복하지 않는다.
- 구현 전에는 조사, 계획, 명세를 구분해서 정리한다.
- 구현 중에는 기존 코드 스타일, 로컬 헬퍼, 타입 규칙, 테스트 구조를 따른다.
- 테스트, 타입 체크, 린트, 빌드 등 검증 작업은 계획에 포함한다.
- 기록은 형용사와 부사를 줄이고 사실 중심으로 작성한다.

---

## 2. 브랜치 규칙

- 작업 브랜치는 항상 `feature/...` 형식을 사용한다.
- 현재 브랜치가 `main` 또는 `master`이면 구현 시작 전에 새 브랜치를 생성하고 전환한다.
- 브랜치 이름은 `feature/{번호}-{phase제목}` 형식을 사용한다.

예시:

```bash
feature/2-auth-login
feature/3-payment-api
```

---

## 3. 문서 역할

### 3.1 문서 트리

프로젝트 문서는 최상위와 영역 폴더가 같은 4종 세트를 가진다. 최상위 문서는 전체 개요와 영역 간 결정을 담고, 세부 내용은 해당 영역 문서에 위임한다.

```text
funchat/
├── README.md
├── RESEARCH.md
├── PLAN.md
├── SPEC.md
├── backend/
│   ├── README.md
│   ├── RESEARCH.md
│   ├── PLAN.md
│   └── SPEC.md
├── frontend/
│   ├── README.md
│   ├── RESEARCH.md
│   ├── PLAN.md
│   └── SPEC.md
├── deploy/
│   ├── README.md
│   ├── RESEARCH.md
│   ├── PLAN.md
│   └── SPEC.md
└── monitoring/
    ├── README.md
    ├── RESEARCH.md
    ├── PLAN.md
    └── SPEC.md
```

### 3.2 위임 규칙

| 계층                                                     | 기록 범위                                                                         |
| -------------------------------------------------------- | --------------------------------------------------------------------------------- |
| 최상위 (`/`)                                             | 프로젝트 목적, 전체 아키텍처, 영역 간 경계, 공통 결정, 자식 문서 링크             |
| 영역 (`backend/`, `frontend/`, `deploy/`, `monitoring/`) | 해당 영역의 실행 방법, API·UI·인프라 명세, 환경변수, 영역별 todo, 영역별 제한사항 |

작업 규칙:

- 상세 내용은 해당 영역 문서에 작성하고, 최상위 문서에는 요약과 링크만 남긴다.
- 같은 내용을 상위·하위 문서에 중복 기록하지 않는다.
- 영역 단위 변경은 해당 영역 문서를 먼저 갱신한다.
- 여러 영역에 걸치는 변경은 영향 받는 영역 문서를 갱신한 뒤, 필요할 때만 최상위 문서를 갱신한다.
- 영역 문서가 없으면 해당 영역 작업 시작 전에 4종 문서 골격을 만든다.

### 3.3 영역 담당 범위

| 영역          | 담당 내용                                                                               |
| ------------- | --------------------------------------------------------------------------------------- |
| `backend/`    | REST API, JWT 인증, STOMP/WebSocket, JPA·MongoDB·Redis, LiveKit 세션·토큰 orchestration |
| `frontend/`   | React UI, STOMP 클라이언트, LiveKit WebRTC 참가 UI                                      |
| `deploy/`     | Docker Compose, Nginx, rolling deploy, LiveKit·인프라 배포 구성                         |
| `monitoring/` | Prometheus, Grafana, exporter, k6 부하 테스트                                           |

### 3.4 문서 종류별 역할

| 파일                                        | 역할                                                                                                                     |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `README.md`                                 | 프로젝트 입문 문서. 목적, 주요 기능, 실행 방법, 개발 환경, 디렉터리 구조, 운영 주의사항을 기록한다.                      |
| `RESEARCH.md`                               | 구현 전 조사 문서. 공식 문서, 참조 구현, 라이브러리 비교, API 제약, 대안 비교, 선택 근거를 기록한다.                     |
| `PLAN.md`                                   | 앞으로 할 일 문서. 구현 범위, 단계별 todo, 테스트 계획, 검증 방법, 결정 필요 사항을 기록한다.                            |
| `SPEC.md`                                   | 현재 구현 명세 문서. 실제 구현된 기능의 동작, 입출력, 상태 전이, 권한, 에러 처리, 외부 연동, 알려진 제한사항을 기록한다. |
| `.codex/markdown/{영역}/phase-{번호}/implementation.md` | 해당 영역 `PLAN.md` Phase 완료 시 구현 요약을 기록한다. |
| `.codex/markdown/{영역}/phase-{번호}/verification.md` | 해당 영역 `PLAN.md` Phase 완료 시 검증 결과를 기록한다. |

`{영역}`은 `backend`, `frontend`, `deploy`, `monitoring` 중 하나다. `{번호}`는 해당 영역 `PLAN.md`에 정의한 Phase 번호다.

최상위 `SPEC.md`는 영역 간 통합 관점의 요약을 담고, API·UI·인프라 상세는 각 영역 `SPEC.md`에 기록한다.

### 3.5 구현·검증 기록 트리

Phase는 최상위가 아니라 **각 영역 `PLAN.md`** 에 정의한다. 영역 Plan의 Phase가 끝나면 해당 영역 폴더에 implementation·verification을 작성한다.

```text
.codex/markdown/
├── backend/
│   └── phase-{번호}/
│       ├── implementation.md
│       └── verification.md
├── frontend/
│   └── phase-{번호}/
│       ├── implementation.md
│       └── verification.md
├── deploy/
│   └── phase-{번호}/
│       ├── implementation.md
│       └── verification.md
└── monitoring/
    └── phase-{번호}/
        ├── implementation.md
        └── verification.md
```

기록 규칙:

- 한 Phase 작업이 여러 영역에 걸치면, 영향 받는 **각 영역**에 implementation·verification을 각각 작성한다.
- 최상위 `PLAN.md`는 영역 간 우선순위와 Phase 개요만 담고, 구현·검증 상세는 영역별 `.codex/markdown/{영역}/`에 기록한다.
- 완료된 todo의 상세 내용은 해당 영역 `PLAN.md`에서 제거하고 implementation으로 옮긴다.

---

## 4. 문서 작성 기준

### `README.md`

- 담당 개발자가 아니어도 프로젝트를 이해할 수 있도록 작성한다.
- 프로젝트 목적, 주요 기능, 실행 방법, 개발 환경, 디렉터리 구조, 운영 주의사항을 기록한다.
- 최상위 문서는 영역별 `README.md` 링크로 세부 실행 방법을 위임한다.
- 영역 문서는 해당 폴더 기준 실행 방법과 진입점을 기록한다.
- 프로젝트 이해에 필요한 정보가 바뀌면 갱신한다.

### `RESEARCH.md`

- 구현 전에 조사한 근거를 기록한다.
- 공식 문서, 인터넷 검색, 참조 구현, 라이브러리 비교, API 제약, 시스템 제약, 중요한 발견 사항을 기록한다.
- 여러 접근 방식의 장단점을 비교한다.
- 선택하거나 배제한 이유를 남긴다.
- 영역 간 공통 결정은 최상위에, 기술·라이브러리·인프라 상세 비교는 해당 영역에 기록한다.

### `PLAN.md`

- 앞으로 진행할 작업만 기록한다.
- 단계별 todo와 체크리스트를 관리한다.
- 테스트 계획과 검증 방법을 포함한다.
- 사용자의 인라인 주석과 피드백이 있으면 반영한다.
- 최상위 `PLAN.md`는 Phase 개요와 영역 간 우선순위를 담고, 영역별 구현 todo는 해당 영역 `PLAN.md`에 기록한다.
- 영역 `PLAN.md`의 Phase todo가 완료되면 상세 내용은 `.codex/markdown/{영역}/phase-{번호}/implementation.md`로 옮긴다.

### `SPEC.md`

- 현재 코드 기준으로 실제 구현된 기능만 기록한다.
- 사용자 관점의 기능 동작을 기록한다.
- 입력, 출력, 상태 전이, 권한, 에러 처리, 외부 연동, 알려진 제한사항을 기록한다.
- 최상위 `SPEC.md`는 사용자 흐름과 영역 간 계약을 담고, 엔드포인트·컴포넌트·배포 구성 상세는 해당 영역 `SPEC.md`에 기록한다.
- 기능 정의가 바뀌면 갱신한다.

---

## 5. Phase 기준

- Phase는 **각 영역 `PLAN.md`** 의 단계를 기준으로 한다. 예: `backend/PLAN.md`의 `1단계`.
- 최상위 `PLAN.md`는 영역 간 목표와 우선순위를 정리하고, 영역별 세부 Phase·todo는 해당 영역 `PLAN.md`에 둔다.
- 영역 `PLAN.md`의 한 Phase todo가 모두 완료되면 `.codex/markdown/{영역}/phase-{번호}/`에 implementation·verification을 작성한다.
- 해당 Phase에 더 진행할 todo가 없으면 merge 내용을 요약하고 PR을 생성한다.

산출물 경로 예시:

```text
.codex/markdown/backend/phase-1/implementation.md
.codex/markdown/backend/phase-1/verification.md
.codex/markdown/deploy/phase-1/implementation.md
.codex/markdown/deploy/phase-1/verification.md
```

---

## 6. 구현 기록

- 영역 `PLAN.md`의 Phase가 끝날 때 `.codex/markdown/{영역}/phase-{번호}/implementation.md`를 작성하거나 갱신한다.
- 구현 기록에는 완료한 작업, 변경 파일, 주요 설계 판단, 영향 범위를 기록한다.
- 구현된 기능 정의가 바뀌면 해당 영역 `SPEC.md`와 필요 시 최상위 `SPEC.md`를 갱신한다.
- 프로젝트 이해에 필요한 정보가 바뀌면 해당 영역 `README.md`와 필요 시 최상위 `README.md`를 갱신한다.

작성 예시:

```markdown
# Implementation

- `application-test.yml` test profile H2 datasource 설정
- `FakeS3TestConfig` S3 호출 대체
- `AuthServiceTest` 로그인 성공 케이스 추가
```

---

## 7. 검증 기록

- 검증은 영역 `PLAN.md` Phase 마지막에 모아서 수행한다.
- 가능한 범위에서 타입 체크, 테스트, 린트, 빌드를 실행한다.
- 검증 결과는 `.codex/markdown/{영역}/phase-{번호}/verification.md`에 기록한다.
- 실패가 있으면 실패 원인, 수정 내용, 재검증 결과를 기록한다.
- 실행하지 못한 검증이 있으면 사유를 남긴다.

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

- 사용자의 피드백이 있으면 관련 산출물을 수정한다.
- 변경 범위에 맞는 영역 문서를 먼저 갱신하고, 전체 영향이 있으면 최상위 문서도 갱신한다.
- 기능 정의가 바뀌면 해당 영역 `SPEC.md`와 필요 시 최상위 `SPEC.md`를 갱신한다.
- 계획이 바뀌면 해당 영역 `PLAN.md`와 필요 시 최상위 `PLAN.md`를 갱신한다.
- 조사 근거가 추가되면 해당 영역 `RESEARCH.md`와 필요 시 최상위 `RESEARCH.md`를 갱신한다.
- 구현 결과가 바뀌면 해당 영역 `.codex/markdown/{영역}/phase-{번호}/implementation.md`를 갱신한다.
- 검증 결과가 바뀌면 해당 영역 `.codex/markdown/{영역}/phase-{번호}/verification.md`를 갱신한다.

---

## 9. PR 생성 기준

- 영역 `PLAN.md`의 Phase todo가 모두 완료되면 PR을 생성한다. 여러 영역을 함께 다룬 작업이면 영향 받는 각 영역의 implementation·verification을 포함한다.
- PR에는 merge 내용을 요약한다.
- PR에는 해당 영역(또는 영역별) 구현 요약과 검증 결과를 포함한다.
- 미완료 항목, 제한사항, 후속 작업이 있으면 명시한다.

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
