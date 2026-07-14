# Documentation 운영 가이드

이 문서는 FunChat의 **문서 구조와 운영 방식**을 설명한다.

## 문서 트리

프로젝트 문서는 최상위와 영역 폴더가 같은 4종 세트를 가진다.

```text
funchat/
├── README.md
├── RESEARCH.md
├── PLAN.md
├── SPEC.md
├── AGENTS.md
├── docs/
│   └── DOCUMENTATION.md
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

## 위임 규칙(기록 범위)

| 계층                                                    | 기록 범위                                                                         |
| ------------------------------------------------------- | --------------------------------------------------------------------------------- |
| 최상위(`/`)                                             | 프로젝트 목적, 전체 아키텍처, 영역 간 경계, 공통 결정, 자식 문서 링크             |
| 영역(`backend/`, `frontend/`, `deploy/`, `monitoring/`) | 해당 영역의 실행 방법, API·UI·인프라 명세, 환경변수, 영역별 todo, 영역별 제한사항 |

원칙:

- 상세 내용은 해당 영역 문서에 작성하고, 최상위 문서에는 요약과 링크만 남긴다.
- 같은 내용을 상위·하위 문서에 중복 기록하지 않는다.
- 영역 단위 변경은 해당 영역 문서를 먼저 갱신한다.
- 여러 영역에 걸치는 변경은 영향 받는 영역 문서를 갱신한 뒤, 필요할 때만 최상위 문서를 갱신한다.

## 문서별 역할(사람을 위한 설명)

| 파일          | 역할                                                                                    |
| ------------- | --------------------------------------------------------------------------------------- |
| `README.md`   | 프로젝트 입문 문서. 목적, 주요 기능, 실행 방법, 개발 환경, 디렉터리 구조, 운영 주의사항 |
| `RESEARCH.md` | 조사 결과 요약 문서(Phase 완료 시 요약을 적재)                                          |
| `PLAN.md`     | 앞으로 할 일 문서. 구현 범위, 단계별 todo, 테스트 계획, 검증 방법                       |
| `SPEC.md`     | 현재 구현 명세 문서. 실제 동작, 입출력, 상태 전이, 권한, 에러 처리, 외부 연동, 제한사항 |
| `AGENTS.md`   | 에이전트 실행 지침(규칙)                                                                |

## 영역 담당 범위(요약)

| 영역          | 담당 내용                                                                               |
| ------------- | --------------------------------------------------------------------------------------- |
| `backend/`    | REST API, JWT 인증, STOMP/WebSocket, JPA·MongoDB·Redis, LiveKit 세션·토큰 orchestration |
| `frontend/`   | React UI, STOMP 클라이언트, LiveKit WebRTC 참가 UI                                      |
| `deploy/`     | Docker Compose, Nginx, rolling deploy, LiveKit·인프라 배포 구성                         |
| `monitoring/` | Prometheus, Grafana, exporter, k6 부하 테스트                                           |

## Phase 기록 경로

Phase는 각 영역 `PLAN.md`에 정의한다. Phase 단위 산출물은 아래 경로를 사용한다.

```text
.codex/markdown/{영역}/phase-{번호}/
  research.md
  implementation.md
  verification.md
```

## 작성 예시

### 진행 중 조사(Phase research)

`.codex/markdown/backend/phase-2/research.md`:

```markdown
# Research

## LiveKit webhook 서명 검증

- 공식 문서: ...
- 후보: ...
- 선택: HMAC-SHA256, raw body 기준
- 근거: ...
```

### Phase 완료 후 조사 요약(영역 RESEARCH)

`backend/RESEARCH.md`:

```markdown
## Phase 2 — LiveKit webhook

- webhook 서명 검증은 HMAC-SHA256 + raw body 기준으로 처리한다.
- 배제: JSON 재직렬화 후 서명 비교 방식
- 상세: [.codex/markdown/backend/phase-2/research.md](../.codex/markdown/backend/phase-2/research.md)
```
