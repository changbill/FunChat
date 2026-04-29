import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080'

function getPagePayload(responseBody) {
  return responseBody.body || responseBody
}

function getRoomId(room) {
  return room.roomId || room.id
}

export const options = {
  stages: [
    { duration: '30s', target: 50 }, // 50명까지 서서히 증가
    { duration: '1m', target: 50 }, // 50명 유지 (안정성 확인)
    { duration: '30s', target: 1000 }, // 1000명까지 피크 부하
    { duration: '30s', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 페이징을 했으므로 95% 요청은 200ms 이내여야 함
    http_req_failed: ['rate<0.01'], // 에러율 1% 미만 유지
  },
}

export default function () {
  // 1. 방 목록 페이징 조회 (0~20 페이지 중 랜덤)
  const page = Math.floor(Math.random() * 20)
  const roomsRes = http.get(`${BASE_URL}/api/rooms?page=${page}&size=20`)

  let roomsBody
  const isOk = check(roomsRes, {
    'list status is 200': (r) => r.status === 200,
    'has content': (r) => {
      try {
        roomsBody = r.json()
      } catch {
        return false
      }
      const pagePayload = getPagePayload(roomsBody)
      return Array.isArray(pagePayload.content) && pagePayload.content.length > 0
    },
  })

  // 목록 조회에 성공했을 때만 상세 조회 시뮬레이션
  if (isOk) {
    const pagePayload = getPagePayload(roomsBody)
    const rooms = pagePayload.content
    const randomRoom = rooms[Math.floor(Math.random() * rooms.length)]
    const roomId = getRoomId(randomRoom)

    // 2. 선택한 방 상세 조회
    const detailRes = http.get(`${BASE_URL}/api/rooms/${roomId}`)
    check(detailRes, {
      'detail status is 200': (r) => r.status === 200,
    })
  }

  sleep(1) // 1초 대기 후 다음 행동
}
