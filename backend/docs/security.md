# 보안 요구사항

## 예외 및 로깅

- 예외는 도메인 예외 + 공통 예외 핸들러(`GlobalExceptionHandler`)로 분리
- 민감 파라미터는 로그에서 제외
- 접근 제어 실패/인증 실패는 상세 메시지 최소화

## 접근 제어

- 공유 토큰/링크는 만료/1회성 제약 우선 적용 검토
- 개인정보 처리는 기본 비공개 정책 선행

## 엔티티 및 영속성

- 엔티티는 `@Table`, `@Column` 제약(길이/nullable/unique/index/FK)을 명확히 설정
- Soft-delete 적용 시 반드시 마이그레이션 문서화
- `@Enumerated(EnumType.STRING)` 기본 사용
- DB 상태 값은 대문자 enum 문자열로 통일 (`PENDING`, `READY`, `FAILED`, `COMPLETED`)
