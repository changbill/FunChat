# Phase 3 구현

- RoomType(TEXT/VIDEO), nullable room_type 컬럼, 요청 기본값과 응답 필드를 추가했다. 기존 null 컬럼은 TEXT로 처리한다.
- 유형별 DB 필터/페이지네이션, 알 수 없는 유형 400 처리, maxMembers null 검증을 추가했다.
- 동일 방 재입장은 멱등 처리하고 참여 인원 응답은 DB count를 사용한다.
- 일반방의 영상 API를 거부한다. 영상 세션이 있는 방 삭제/마지막 퇴장은 LiveKit 방 및 세션 FK 정리 후 진행한다. 외부 삭제 실패는 트랜잭션 실패이며 404는 이미 삭제된 것으로 처리한다.
- 프런트엔드와 함께 배포해야 한다. 운영 MySQL 컬럼 추가는 기존 ddl-auto=update 경로이며 실제 운영 마이그레이션은 미실행이다.
