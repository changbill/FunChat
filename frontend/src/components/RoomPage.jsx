import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { clearAuth } from '../utils/auth'
import { roomRequest } from '../utils/roomApi'
import ChatContainer from './ChatContainer'

export default function RoomPage() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const [result, setResult] = useState(null)
  useEffect(() => {
    const controller = new AbortController()
    roomRequest(`/${roomId}/enter`, { method: 'POST', signal: controller.signal })
      .then(room => setResult({ roomId, room }))
      .catch(error => {
        if (controller.signal.aborted) return
        if (error.status === 401) { clearAuth(); navigate('/login', { replace: true }); return }
        setResult({ roomId, error: error.message })
      })
    return () => controller.abort()
  }, [roomId, navigate])

  if (result?.roomId !== roomId) return <main className="room-list"><p role="status">채팅방에 입장하고 있습니다…</p></main>
  if (result.error) return <main className="room-list"><p role="alert">{result.error}</p><Link to="/">목록으로 돌아가기</Link></main>
  return <ChatContainer key={roomId} room={result.room} />
}
