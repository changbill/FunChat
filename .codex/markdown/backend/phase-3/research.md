# 채팅방 유형 조사

- Room 유형을 TEXT/VIDEO enum으로 저장한다. nullable 신규 컬럼으로 기존 DB 행과 롤링 중 이전 앱 삽입을 허용하며 null은 TEXT로 해석한다.
- 유형 필터는 DB 페이지네이션 이전에 적용해야 목록 누락/잘못된 페이지 수를 방지한다.
- 영상 토큰 발급은 VIDEO 방으로 제한한다.
- VideoSession의 Room FK 때문에 마지막 참여자 퇴장 시 세션을 먼저 정리해야 방 삭제가 가능하다.
