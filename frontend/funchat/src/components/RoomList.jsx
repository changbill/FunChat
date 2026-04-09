import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import '../css/RoomList.css'
import { clearAuth, getAccessToken } from '../utils/auth'
import RefreshButton from './RefreshButton'

const API_BASE = ''

const RoomList = () => {
  const PAGE_SIZE = 20
  const PAGE_WINDOW = 5

  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState(null)
  /** Spring Pageable: page는 0-based */
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  const navigate = useNavigate()
  const location = useLocation()

  const authHeaders = () => {
    const token = getAccessToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  const handleAuthError = () => {
    clearAuth()
    navigate('/login')
  }

  const enterRoom = (roomId) => {
    navigate(`/room/${roomId}`)
  }

  const loadRooms = useCallback(
    async (mode = 'initial', targetPage = null) => {
      const isInitial = mode === 'initial'
      try {
        if (isInitial) setLoading(true)
        else setRefreshing(true)
        setError(null)
        const nextPage =
          typeof targetPage === 'number' && Number.isFinite(targetPage)
            ? Math.max(0, targetPage)
            : 0

        const path = `${API_BASE}/api/rooms`
        const qs = new URLSearchParams({
          page: String(nextPage),
          size: String(PAGE_SIZE),
        }).toString()
        const url = `${path}?${qs}`

        const res = await fetch(url, {
          credentials: 'include',
          headers: authHeaders(),
        })
        if (res.status === 401 || res.status === 403) {
          clearAuth()
          navigate('/login')
          return
        }
        if (!res.ok) throw new Error('채팅방 목록을 불러오지 못했습니다.')
        const data = await res.json()
        const raw = data?.body
        const parsed = raw && typeof raw === 'object' ? raw : null
        const roomData = Array.isArray(raw)
          ? raw
          : parsed
            ? Array.isArray(parsed.content)
              ? parsed.content
              : Array.isArray(parsed.rooms)
                ? parsed.rooms
                : []
            : []

        const serverTotalPages =
          parsed &&
          typeof parsed.totalPages === 'number' &&
          parsed.totalPages > 0
            ? parsed.totalPages
            : 1
        const serverPage =
          parsed && typeof parsed.number === 'number' && parsed.number >= 0
            ? parsed.number
            : nextPage

        setRooms(roomData)
        setTotalPages(serverTotalPages)
        setPage(serverPage)
      } catch (err) {
        setError(err.message)
        if (isInitial) setRooms([])
      } finally {
        if (isInitial) setLoading(false)
        else setRefreshing(false)
      }
    },
    [API_BASE, PAGE_SIZE, navigate],
  )

  useEffect(() => {
    setPage(0)
    setTotalPages(1)
    loadRooms('initial', 0)
  }, [location.pathname, loadRooms])

  const createRoom = async () => {
    const title = window.prompt('채팅방 제목을 입력하세요.')
    if (!title) return

    try {
      setCreating(true)
      setCreateError(null)
      const res = await fetch(`${API_BASE}/api/rooms`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...authHeaders(),
        },
        body: JSON.stringify({
          title,
          maxMembers: 10,
        }),
      })
      if (res.status === 401 || res.status === 403) {
        handleAuthError()
        return
      }
      if (!res.ok) throw new Error('방 만들기에 실패했습니다.')
      const data = await res.json()
      const roomId = data?.body?.roomId
      if (roomId != null) {
        enterRoom(roomId)
      } else {
        throw new Error('방 정보를 받지 못했습니다.')
      }
    } catch (err) {
      setCreateError(err.message)
    } finally {
      setCreating(false)
    }
  }

  const getRoomInitial = (name) => {
    if (!name || typeof name !== 'string') return '?'
    return name.charAt(0).toUpperCase()
  }

  const enterRoomWithServer = async (roomId) => {
    try {
      const res = await fetch(`${API_BASE}/api/rooms/${roomId}/enter`, {
        method: 'POST',
        credentials: 'include',
        headers: authHeaders(),
      })
      if (res.status === 401 || res.status === 403) {
        handleAuthError()
        return
      }
      if (!res.ok) throw new Error('방 입장에 실패했습니다.')
      enterRoom(roomId)
    } catch (err) {
      setCreateError(err.message)
    }
  }

  const busy = loading || refreshing
  const canGoPrev = page > 0 && !busy
  const canGoNext = page < totalPages - 1 && !busy

  const windowStart = Math.floor(page / PAGE_WINDOW) * PAGE_WINDOW
  const windowEndExclusive = Math.min(windowStart + PAGE_WINDOW, totalPages)
  const pageNumbers = Array.from(
    { length: Math.max(0, windowEndExclusive - windowStart) },
    (_, i) => windowStart + i,
  )

  const goToPage = (p) => {
    const clamped = Math.min(Math.max(0, p), Math.max(0, totalPages - 1))
    if (clamped === page) return
    setPage(clamped)
    loadRooms('refresh', clamped)
  }

  return (
    <section className="room-list">
      <div className="room-list__header-row">
        <h2 className="room-list__header">채팅방 목록</h2>
        <div className="room-list__actions">
          <RefreshButton
            onClick={() => loadRooms('refresh', page)}
            disabled={busy}
            spinning={refreshing}
          />
          <button
            type="button"
            className="room-list__create"
            onClick={createRoom}
            disabled={creating}
          >
            {creating ? '만드는 중...' : '방 만들기'}
          </button>
        </div>
      </div>

      {createError && (
        <p className="room-list__error room-list__error--inline">
          {createError}
        </p>
      )}

      {loading && (
        <p className="room-list__loading">채팅방 목록 불러오는 중...</p>
      )}

      {!loading && error && <p className="room-list__error">{error}</p>}

      {!loading && !error && rooms.length === 0 && (
        <p className="room-list__empty">채팅방이 없습니다.</p>
      )}

      {!loading && !error && rooms.length > 0 && (
        <>
          <ul className="room-list__list">
            {rooms.map((room) => (
              <li key={room.roomId} className="room-list__item">
                <span className="room-list__icon" aria-hidden>
                  {getRoomInitial(room.title ?? `방 ${room.roomId}`)}
                </span>
                <div className="room-list__body">
                  <p className="room-list__name">
                    {room.title ?? `방 ${room.roomId}`}
                  </p>
                  <p className="room-list__meta">
                    인원 {room.currentMembers ?? 0}/{room.maxMembers ?? 0} ·
                    방장 {room.managerNickname ?? '-'}
                  </p>
                </div>
                <button
                  type="button"
                  className="room-list__enter"
                  onClick={() => enterRoomWithServer(room.roomId)}
                >
                  입장
                </button>
              </li>
            ))}
          </ul>

          {totalPages > 1 && (
            <nav
              className="room-list__pagination"
              aria-label="채팅방 목록 페이지네이션"
            >
              <button
                type="button"
                className="room-list__page room-list__page--arrow"
                onClick={() => goToPage(page - 1)}
                disabled={!canGoPrev}
                aria-label="이전 페이지"
              >
                ‹
              </button>

              {pageNumbers.map((p) => (
                <button
                  key={p}
                  type="button"
                  className={`room-list__page ${p === page ? 'room-list__page--active' : ''}`}
                  onClick={() => goToPage(p)}
                  disabled={busy}
                  aria-current={p === page ? 'page' : undefined}
                >
                  {p + 1}
                </button>
              ))}

              <button
                type="button"
                className="room-list__page room-list__page--arrow"
                onClick={() => goToPage(page + 1)}
                disabled={!canGoNext}
                aria-label="다음 페이지"
              >
                ›
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}

export default RoomList
