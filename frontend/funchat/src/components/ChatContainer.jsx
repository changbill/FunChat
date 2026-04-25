import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import MessageInput from './MessageInput'
import MessageList from './MessageList'
import SockJS from 'sockjs-client'
import { over } from 'stompjs'
import { getAccessToken } from '../utils/auth'
import { API_BASE } from '../utils/http'
import { normalizeChatMessage, parseStompBody } from '../utils/chatMessage'
import '../css/ChatRoom.css'

const ChatContainer = () => {
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
          if (!res.ok) leaveSentRef.current = false
        })
        .catch(() => {
          leaveSentRef.current = false
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
    const onPageHide = () => {
      void callLeaveApi(true)
    }
    window.addEventListener('pagehide', onPageHide)
    return () => {
      window.removeEventListener('pagehide', onPageHide)
      void callLeaveApi(true)
    }
  }, [callLeaveApi])

  const sendMessage = () => {
    const client = stompClientRef.current
    if (client && inputMessage.trim()) {
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
    await callLeaveApi(false)

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
    <section className="chat-room">
      <header className="chat-room__header">
        <h1 className="chat-room__title">채팅방</h1>
        <button
          type="button"
          className="chat-room__leave"
          onClick={endConnection}
        >
          방 나가기
        </button>
      </header>

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
        />
      </div>
    </section>
  )
}

export default ChatContainer
