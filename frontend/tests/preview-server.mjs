// Isolated UI fixture. Never connects to production services or real credentials.
import http from 'node:http'
import { createServer } from 'vite'

let rooms = [
  { roomId: 1, title: '오늘의 소소한 이야기', roomType: 'TEXT', currentMembers: 3, maxMembers: 10, managerNickname: '지우' },
  { roomId: 2, title: '퇴근 후 함께하는 영상 수다', roomType: 'VIDEO', currentMembers: 2, maxMembers: 6, managerNickname: '하늘' },
]
const token = `test.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.test`
const api = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost')
  const send = (body, code = 200, message = '성공') => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ code, message, body })) }
  if (url.pathname === '/api/auth/login') return send({ accessToken: token, refreshToken: 'test', nickname: '테스터' })
  if (url.pathname === '/api/auth/logout') return send(null)
  if (url.pathname === '/api/rooms' && req.method === 'GET') {
    const content = rooms.filter(room => !url.searchParams.get('roomType') || room.roomType === url.searchParams.get('roomType'))
    return send({ content, totalPages: 1, number: 0 })
  }
  if (url.pathname === '/api/rooms' && req.method === 'POST') {
    let raw = ''; for await (const chunk of req) raw += chunk
    const body = JSON.parse(raw)
    if (!body.title?.trim() || !['TEXT', 'VIDEO'].includes(body.roomType) || body.maxMembers < 2) return send(null, 400, '입력값을 확인해 주세요.')
    const room = { ...body, roomId: rooms.length + 1, currentMembers: 1, managerNickname: '테스터' }
    rooms.push(room); return send(room)
  }
  const match = url.pathname.match(/^\/api\/rooms\/(\d+)\/(enter|leave|video\/token)$/)
  if (match) {
    const room = rooms.find(item => item.roomId === Number(match[1]))
    if (!room) return send(null, 404, '방을 찾을 수 없습니다.')
    if (match[2] === 'video/token') return send(null, 502, '테스트 영상 서버에 연결할 수 없습니다. 다시 시도해 주세요.')
    if (match[2] === 'leave') return send(null)
    return send(room)
  }
  if (url.pathname.startsWith('/api/chat/messages/')) return send({ messages: [], hasNext: false })
  send(null, 404, 'UI fixture: endpoint unavailable')
})
await new Promise(resolve => api.listen(18080, '127.0.0.1', resolve))
const vite = await createServer({ server: { port: 5173, strictPort: true, host: '127.0.0.1', proxy: { '/api': 'http://127.0.0.1:18080', '/ws': 'http://127.0.0.1:18080' } } })
await vite.listen()
console.log('UI fixture ready: http://127.0.0.1:5173 — log in with any test email/password')
