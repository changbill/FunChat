import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  stages: [
    { duration: '30s', target: 50 }, // 50명까지 서서히 증가
    { duration: '1m', target: 50 }, // 50명 유지 (안정성 확인)
    { duration: '30s', target: 1000 }, // 100명까지 피크 부하
    { duration: '30s', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 페이징을 했으므로 95% 요청은 200ms 이내여야 함
    http_req_failed: ['rate<0.01'], // 에러율 1% 미만 유지
  },
}

const BASE_URL = 'http://host.docker.internal:8080'

export default function () {
  // 1. 방 목록 페이징 조회 (0~20 페이지 중 랜덤)
  const page = Math.floor(Math.random() * 20)
  const roomsRes = http.get(`${BASE_URL}/api/rooms?page=${page}&size=20`)

  const isOk = check(roomsRes, {
    'list status is 200': (r) => r.status === 200,
    'has content': (r) => {
      const body = JSON.parse(r.body)
      // ResponseUtil을 쓰신다면 구조에 따라 body.data.content 등이 될 수 있습니다.
      // 일반적인 Page 구조라면 body.content 입니다.
      return body.content && body.content.length > 0
    },
  })

  // 목록 조회에 성공했을 때만 상세 조회 시뮬레이션
  if (isOk) {
    const body = JSON.parse(roomsRes.body)
    const rooms = body.content
    const randomRoom = rooms[Math.floor(Math.random() * rooms.length)]

    // 2. 선택한 방 상세 조회
    const detailRes = http.get(`${BASE_URL}/api/rooms/${randomRoom.id}`)
    check(detailRes, {
      'detail status is 200': (r) => r.status === 200,
    })
  }

  sleep(1) // 1초 대기 후 다음 행동
}
