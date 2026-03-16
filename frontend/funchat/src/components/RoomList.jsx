import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import '../css/RoomList.css'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

const RoomList = () => {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState(null)

  const navigate = useNavigate()

  const enterRoom = (roomId) => {
    navigate(`/room/${roomId}`)
  }

  const createRoom = async () => {
    try {
      setCreating(true)
      setCreateError(null)
      const res = await fetch(`${API_BASE}/api/room`, { method: 'GET' })
      if (!res.ok) throw new Error('방 만들기에 실패했습니다.')
      const data = await res.json()
      const roomId = data?.id ?? data?.roomId
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

  useEffect(() => {
    const fetchRooms = async () => {
      try {
        setLoading(true)
        setError(null)
        const res = await fetch(`${API_BASE}/api/rooms`)
        if (!res.ok) throw new Error('채팅방 목록을 불러오지 못했습니다.')
        const data = await res.json()
        setRooms(Array.isArray(data) ? data : (data.rooms ?? []))
      } catch (err) {
        setError(err.message)
        setRooms([])
      } finally {
        setLoading(false)
      }
    }
    fetchRooms()
  }, [])

  if (loading) {
    return (
      <section className="room-list">
        <div className="room-list__header-row">
          <h2 className="room-list__header">채팅방 목록</h2>
          <button
            type="button"
            className="room-list__create"
            onClick={createRoom}
            disabled={creating}
          >
            {creating ? '만드는 중...' : '방 만들기'}
          </button>
        </div>
        <p className="room-list__loading">채팅방 목록 불러오는 중...</p>
      </section>
    )
  }

  if (error) {
    return (
      <section className="room-list">
        <div className="room-list__header-row">
          <h2 className="room-list__header">채팅방 목록</h2>
          <button
            type="button"
            className="room-list__create"
            onClick={createRoom}
            disabled={creating}
          >
            {creating ? '만드는 중...' : '방 만들기'}
          </button>
        </div>
        <p className="room-list__error">{error}</p>
      </section>
    )
  }

  if (rooms.length === 0) {
    return (
      <section className="room-list">
        <div className="room-list__header-row">
          <h2 className="room-list__header">채팅방 목록</h2>
          <button
            type="button"
            className="room-list__create"
            onClick={createRoom}
            disabled={creating}
          >
            {creating ? '만드는 중...' : '방 만들기'}
          </button>
        </div>
        {createError && <p className="room-list__error room-list__error--inline">{createError}</p>}
        <p className="room-list__empty">채팅방이 없습니다.</p>
      </section>
    )
  }

  return (
    <section className="room-list">
      <div className="room-list__header-row">
        <h2 className="room-list__header">채팅방 목록</h2>
        <button
          type="button"
          className="room-list__create"
          onClick={createRoom}
          disabled={creating}
        >
          {creating ? '만드는 중...' : '방 만들기'}
        </button>
      </div>
      {createError && <p className="room-list__error room-list__error--inline">{createError}</p>}
      <ul className="room-list__list">
        {rooms.map((room) => (
          <li key={room.id} className="room-list__item">
            <span className="room-list__icon" aria-hidden>
              {getRoomInitial(room.name ?? `방 ${room.id}`)}
            </span>
            <div className="room-list__body">
              <p className="room-list__name">{room.name ?? `방 ${room.id}`}</p>
              <p className="room-list__meta">
                인원 {room.participantCount ?? 0}명
              </p>
            </div>
            <button
              type="button"
              className="room-list__enter"
              onClick={() => enterRoom(room.id)}
            >
              입장
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default RoomList
