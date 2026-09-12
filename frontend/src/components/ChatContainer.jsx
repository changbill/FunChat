import { lazy, Suspense, useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import MessageInput from './MessageInput'
import MessageList from './MessageList'
import LogoutButton from './LogoutButton'
import SockJS from 'sockjs-client'
import { over } from 'stompjs'
import { getAccessToken } from '../utils/auth'
import { API_BASE } from '../utils/http'
import { normalizeChatMessage, parseStompBody } from '../utils/chatMessage'
import '../css/ChatRoom.css'
const VideoPanel = lazy(() => import('./VideoPanel'))

const ChatContainer = ({ room }) => {
  const isVideo = room?.roomType === 'VIDEO'
  const [connected, setConnected] = useState(false)
  const [chatError, setChatError] = useState('')
  const [leaving, setLeaving] = useState(false)
  /** setState로 STOMP 클라이언트를 두면 연결 시마다 리렌더 → effect 재실행 → "연결 시도" 반복될 수 있음 */
  const stompClientRef = useRef(null)
  const [receivedMessages, setReceivedMessages] = useState([])
  const [inputMessage, setInputMessage] = useState('')
  const { roomId } = useParams()
  const navigate = useNavigate()
  /** 같은 방에서 leave가 중복되면 백엔드에서 오류 가능 → 한 번만 전송 */
  const leaveSentRef = useRef(false)
  /** GET 히스토리 다음 페이지 커서·hasNext (스크롤 상단에서 과거 메시지 로드) */
  const historyCursorRef = useRef(null)
  const hasMoreHistoryRef = useRef(false)
  const loadingOlderRef = useRef(false)
  const scrollContainerRef = useRef(null)
  const [loadingOlder, setLoadingOlder] = useState(false)

  useEffect(() => {
    leaveSentRef.current = false
  }, [roomId])

  const callLeaveApi = useCallback(
    (keepalive = false) => {
      if (leaveSentRef.current) return Promise.resolve()
      const token = getAccessToken()
      if (!roomId || !token) return Promise.resolve()
      leaveSentRef.current = true
      return fetch(`${API_BASE}/api/rooms/${roomId}/leave`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        keepalive,
      })
        .then((res) => {
          if (!res.ok) { leaveSentRef.current = false; throw new Error('방에서 나가지 못했습니다. 잠시 후 다시 시도해 주세요.') }
        })
        .catch((error) => {
          leaveSentRef.current = false
          throw error
        })
    },
    [roomId],
  )

  const loadOlderMessages = useCallback(async () => {
    if (loadingOlderRef.current) return
    if (!hasMoreHistoryRef.current || !historyCursorRef.current) return
    const accessToken = getAccessToken()
    if (!roomId || !accessToken) return

    const container = scrollContainerRef.current
    const prevScrollHeight = container?.scrollHeight ?? 0
    const prevScrollTop = container?.scrollTop ?? 0

    loadingOlderRef.current = true
    setLoadingOlder(true)

    try {
      const cursor = encodeURIComponent(historyCursorRef.current)
      const res = await fetch(
        `${API_BASE}/api/chat/messages/${roomId}?cursorId=${cursor}&size=100`,
        {
          credentials: 'include',
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
          signal: AbortSignal.timeout?.(15_000),
        },
      )
      if (!res.ok) return

      const data = await res.json()
      const body = data?.body ?? {}
      const list = body.messages ?? []
      historyCursorRef.current = body.nextCursorId ?? null
      hasMoreHistoryRef.current = Boolean(body.hasNext)

      const normalized = [...list]
        .reverse()
        .map(normalizeChatMessage)
        .filter(Boolean)
      if (!normalized.length) return

      setReceivedMessages((prev) => [...normalized, ...prev])

      requestAnimationFrame(() => {
        const c = scrollContainerRef.current
        if (c) {
          c.scrollTop = prevScrollTop + (c.scrollHeight - prevScrollHeight)
        }
      })
    } catch {
      /* 네트워크 등 */
    } finally {
      loadingOlderRef.current = false
      setLoadingOlder(false)
    }
  }, [roomId])

  const handleHistoryScroll = useCallback(
    (e) => {
      const el = e.currentTarget
      if (el.scrollTop > 80) return
      void loadOlderMessages()
    },
    [loadOlderMessages],
  )

  /**
   * 히스토리 fetch와 STOMP 연결을 같은 effect에서 순서대로 처리한다.
   * (히스토리를 나중에 set하면 실시간으로 받은 메시지가 덮어씌워지는 레이스가 난다)
   */
  useEffect(() => {
    const accessToken = getAccessToken()
    if (!roomId || !accessToken) return

    let cancelled = false
    /** effect 초기에 SockJS를 만들면 Strict Mode cleanup이 connect보다 먼저 socket.close → onopen 영구 미호출 */
    let socket = null
    let client = null

    const connectAndSubscribe = () => {
      if (cancelled) return
      socket = new SockJS(`${API_BASE}/ws`)
      client = over(socket)
      client.debug = () => {}

      client.connect(
        {
          Authorization: `Bearer ${accessToken}`,
        },
        () => {
          if (cancelled) return
          if (import.meta.env.DEV) {
            console.info('[STOMP] 연결됨', { roomId })
          }
          stompClientRef.current = client
          setConnected(true)
          setChatError('')
          client.subscribe(
            `/sub/chat/${roomId}`,
            (message) => {
              const raw = parseStompBody(message.body)
              const next = raw ? normalizeChatMessage(raw) : null
              if (next) {
                setReceivedMessages((prev) => [...prev, next])
              }
            },
            {
              Authorization: `Bearer ${accessToken}`,
            },
          )
        },
        (error) => {
          if (!cancelled) { setConnected(false); setChatError('채팅 연결이 끊겼습니다. 새로고침하여 다시 연결해 주세요.') }
          if (!cancelled && import.meta.env.DEV) {
            console.warn('STOMP 연결 실패', error)
          }
        },
      )
    }

    const run = async () => {
      setReceivedMessages([])
      historyCursorRef.current = null
      hasMoreHistoryRef.current = false
      try {
        const res = await fetch(
          `${API_BASE}/api/chat/messages/${roomId}?size=100`,
          {
            credentials: 'include',
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
            signal: AbortSignal.timeout?.(15_000),
          },
        )
        if (!cancelled && res.ok) {
          const data = await res.json()
          const body = data?.body ?? {}
          const list = body.messages ?? []
          historyCursorRef.current = body.nextCursorId ?? null
          hasMoreHistoryRef.current = Boolean(body.hasNext)
          const normalized = [...list]
            .reverse()
            .map(normalizeChatMessage)
            .filter(Boolean)
          setReceivedMessages(normalized)
        }
      } catch {
        /* Mongo 지연·네트워크 오류 등: 히스토리 없이도 소켓은 연결 */
      }
      if (!cancelled) {
        connectAndSubscribe()
      }
    }

    void run()

    return () => {
      cancelled = true
      stompClientRef.current = null
      try {
        if (client?.connected) {
          client.disconnect(() => {})
        }
      } catch {
        /* ignore */
      }
      try {
        socket?.close()
      } catch {
        /* ignore */
      }
    }
  }, [roomId])

  useEffect(() => {
    return () => {
      void callLeaveApi(true).catch(() => {})
    }
  }, [callLeaveApi])

  const sendMessage = () => {
    const client = stompClientRef.current
    if (client?.connected && inputMessage.trim()) {
      const text = inputMessage.trim()
      client.send(
        '/pub/chat/message',
        {
          roomId: String(roomId),
          Authorization: `Bearer ${getAccessToken()}`,
        },
        JSON.stringify({
          roomId: Number(roomId),
          content: text,
          type: 'TEXT',
        }),
      )
      setInputMessage('')
    }
  }

  const endConnection = async () => {
    if (leaving) return
    setLeaving(true)
    try { await callLeaveApi(false) } catch (error) { setChatError(error.message); setLeaving(false); return }

    const client = stompClientRef.current
    if (client) {
      try {
        client.disconnect(() => {})
      } catch {
        /* ignore */
      }
      stompClientRef.current = null
    }
    navigate('/')
  }

  return (
    <section className={`chat-room ${isVideo ? 'chat-room--video' : ''}`}>
      <header className="chat-room__header">
        <div><p className="room-list__eyebrow">{isVideo ? '영상 채팅방' : '일반 채팅방'}</p><h1 className="chat-room__title">{room?.title ?? '채팅방'}</h1></div>
        <div className="chat-room__actions">
          <button
            type="button"
            className="chat-room__leave"
            onClick={endConnection}
            disabled={leaving}
          >
            {leaving ? '나가는 중…' : '방 나가기'}
          </button>
          <LogoutButton
            className="chat-room__logout"
            beforeLogout={() => callLeaveApi(false)}
          />
        </div>
      </header>

      {isVideo && <Suspense fallback={<p role="status">영상통화 화면 준비 중…</p>}><VideoPanel roomId={roomId} /></Suspense>}
      <p className="chat-room__connection" role="status">{connected ? '● 실시간 채팅 연결됨' : chatError ? '채팅 연결 안 됨' : '채팅 연결 중…'}</p>
      {chatError && <p className="room-list__error" role="alert">{chatError}</p>}

      <div className="chat-room__body">
        <div
          ref={scrollContainerRef}
          className="chat-room__scroll"
          onScroll={handleHistoryScroll}
        >
          {loadingOlder ? (
            <p className="chat-room__loading-older">이전 메시지 불러오는 중…</p>
          ) : null}
          <MessageList messages={receivedMessages} />
        </div>
      </div>

      <div className="chat-room__composer">
        <MessageInput
          inputMessage={inputMessage}
          setInputMessage={setInputMessage}
          sendMessage={sendMessage}
          disabled={!connected}
        />
      </div>
    </section>
  )
}

export default ChatContainer
