## AGENTS

문서 구조/운영 방식 설명은 `docs/DOCUMENTATION.md`로 참고한다.

## 1. 작업 시작 규칙

- 작업 시작 전, 변경 범위에 해당하는 `README.md`, `RESEARCH.md`, `PLAN.md`, `SPEC.md`를 확인한다.
- 변경 범위를 `backend`, `frontend`, `deploy`, `monitoring` 중 하나(또는 복수)로 식별한다.
- 같은 내용을 최상위 문서와 영역 문서에 중복 기록하지 않는다.

## 2. 브랜치 규칙

- 작업 브랜치는 항상 `feature/...` 형식을 사용한다.
- 현재 브랜치가 `main` 또는 `master`이면 구현 시작 전에 새 브랜치를 생성하고 전환한다.
- 브랜치 이름은 `feature/{번호}-{phase제목}` 형식을 사용한다.

## 3. 문서 갱신 규칙

- 영역 단위 변경은 해당 영역 문서를 먼저 갱신한다.
- 여러 영역에 걸치는 변경은 영향 받는 각 영역 문서를 먼저 갱신한 뒤, 필요할 때만 최상위 문서를 갱신한다.
- 실행 방법/개발 환경/디렉터리 구조가 바뀌면 `README.md`를 갱신한다.
- 앞으로 할 일이 바뀌면 `PLAN.md`를 갱신한다.
- 실제 동작/계약/에러 처리가 바뀌면 `SPEC.md`를 갱신한다.

## 4. 조사(Research) 기록 규칙

- Phase별 조사는 `.codex/markdown/{영역}/phase-{번호}/research.md`를 기본 작성 위치로 사용한다.
- Phase가 완료되면 `research.md` 내용을 요약해 `{영역}/RESEARCH.md`에 정리한다.
- 영역 간 공통 결정은 최상위 `RESEARCH.md`에도 요약한다.
- 같은 내용을 `{영역}/RESEARCH.md`와 `research.md`에 중복 기록하지 않는다.

## 5. Phase 산출물 규칙

- Phase 구현 완료 후 `.codex/markdown/{영역}/phase-{번호}/implementation.md`를 작성/갱신한다.
- Phase 검증 완료 후 `.codex/markdown/{영역}/phase-{번호}/verification.md`를 작성/갱신한다.
- Phase todo가 모두 완료되면 필요한 문서 갱신을 마치고 PR을 생성한다.

## 6. 검증 규칙

- 검증은 영역 `PLAN.md` Phase 마지막에 모아서 수행한다.
- 가능한 범위에서 타입 체크, 테스트, 린트, 빌드를 실행한다.
- 실패가 있으면 실패 원인, 수정 내용, 재검증 결과를 기록한다.
- 실행하지 못한 검증이 있으면 사유를 기록한다.

## 7. PR 규칙

- PR에는 변경 요약을 포함한다.
- PR에는 해당 영역(또는 영역별) 구현 요약과 검증 결과를 포함한다.
- 미완료 항목, 제한사항, 후속 작업이 있으면 명시한다.
