import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { clearAuth } from '../utils/auth'
import { roomRequest } from '../utils/roomApi'
import LogoutButton from './LogoutButton'
import RefreshButton from './RefreshButton'
import '../css/RoomList.css'

const TYPES = { TEXT: '일반 채팅', VIDEO: '영상 채팅' }

export default function RoomList() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [roomType, setRoomType] = useState('TEXT')
  const [title, setTitle] = useState('')
  const [maxMembers, setMaxMembers] = useState(10)
  const createLock = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    const query = new URLSearchParams({ page: String(page), size: '20' })
    if (filter) query.set('roomType', filter)
    roomRequest(`?${query}`, { signal: controller.signal })
      .then(result => {
        if (controller.signal.aborted) return
        setData(result); setError(''); setLoading(false)
      })
      .catch(err => {
        if (controller.signal.aborted) return
        if (err.status === 401) { clearAuth(); navigate('/login'); return }
        setError(err.message); setLoading(false)
      })
    return () => controller.abort()
  }, [filter, page, revision, navigate])

  const changePage = next => { setLoading(true); setPage(next) }
  const selectFilter = type => { setLoading(true); setFilter(type); setPage(0); setRevision(value => value + 1) }
  const createRoom = async event => {
    event.preventDefault()
    if (createLock.current || !title.trim()) return
    createLock.current = true; setCreating(true); setCreateError('')
    try {
      const room = await roomRequest('', { method: 'POST', body: { title: title.trim(), maxMembers: Number(maxMembers), roomType } })
      navigate(`/room/${room.roomId}`)
    } catch (err) {
      if (err.status === 401) { clearAuth(); navigate('/login'); return }
      setCreateError(err.message)
    } finally { setCreating(false); createLock.current = false }
  }

  const rooms = data?.content ?? []
  return (
    <main className="room-list">
      <header className="room-list__header-row">
        <div><p className="room-list__eyebrow">FUNCHAT / LOUNGE</p><h1 className="room-list__header">함께 나누는 대화</h1></div>
        <div className="room-list__actions">
          <RefreshButton onClick={() => { setLoading(true); setRevision(value => value + 1) }} disabled={loading} spinning={loading} />
          <LogoutButton className="room-list__logout" />
        </div>
      </header>
      <section className="room-list__hero">
        <div><span className="room-list__eyebrow">어떤 대화를 시작할까요?</span><h2>글로 편하게, 얼굴 보며 가깝게.</h2><p>일반 채팅으로 이야기를 나누거나 영상방에서 만나보세요.</p></div>
        <button className="room-list__enter" onClick={() => { setFormOpen(!formOpen); setCreateError('') }} aria-expanded={formOpen} aria-controls="create-room">{formOpen ? '접기' : '+ 새 채팅방'}</button>
      </section>
      {formOpen && <form id="create-room" className="room-create" onSubmit={createRoom}>
        <h2>새로운 대화 시작하기</h2>
        <fieldset disabled={creating}><legend>채팅방 종류</legend>
          <div className="room-create__types">{Object.entries(TYPES).map(([type, label]) => <label key={type} className={roomType === type ? 'selected' : ''}>
            <input type="radio" name="roomType" value={type} checked={roomType === type} onChange={() => setRoomType(type)} />
            <strong>{label}</strong><span>{type === 'VIDEO' ? '카메라·마이크와 텍스트로 함께' : '메시지로 가볍게 이야기하기'}</span>
          </label>)}</div>
          <div className="room-create__fields"><label>방 제목<input autoFocus required maxLength={100} value={title} onChange={event => setTitle(event.target.value)} placeholder="오늘은 어떤 이야기를 나눌까요?" /></label>
            <label>최대 인원<input required type="number" min="2" max="100" value={maxMembers} onChange={event => setMaxMembers(event.target.value)} /></label></div>
        </fieldset>
        {createError && <p role="alert" className="room-list__error">{createError}</p>}
        <button className="room-list__enter" disabled={creating || !title.trim()}>{creating ? '만드는 중…' : `${TYPES[roomType]}방 만들기`}</button>
      </form>}
      <div className="room-list__filters" role="group" aria-label="채팅방 종류 필터">
        {[['', '전체'], ...Object.entries(TYPES)].map(([type, label]) => <button key={type} aria-pressed={filter === type} onClick={() => selectFilter(type)}>{label}</button>)}
      </div>
      {loading ? <p className="room-list__empty" role="status">대화 공간을 불러오는 중…</p>
        : error ? <p className="room-list__error" role="alert">{error}</p>
          : rooms.length === 0 ? <div className="room-list__empty"><h2>아직 열린 채팅방이 없어요</h2><p>새 채팅방을 만들고 첫 대화를 시작해 보세요.</p></div>
            : <ul className="room-list__list">{rooms.map(room => <li key={room.roomId} className="room-list__item">
              <span className={`room-list__icon ${room.roomType === 'VIDEO' ? 'room-list__icon--video' : ''}`} aria-hidden="true">{room.roomType === 'VIDEO' ? '◉' : '#'}</span>
              <div className="room-list__body"><span className="room-list__badge">{TYPES[room.roomType] ?? TYPES.TEXT}</span><h2 className="room-list__name">{room.title}</h2><p className="room-list__meta">{room.currentMembers} / {room.maxMembers}명 · 방장 {room.managerNickname}</p></div>
              <button className="room-list__enter" onClick={() => navigate(`/room/${room.roomId}`)} aria-label={`${room.title} 입장`}>입장 →</button>
            </li>)}</ul>}
      {data.totalPages > 1 && <nav className="room-list__pagination" aria-label="채팅방 페이지">
        <button className="room-list__page" disabled={loading || page === 0} onClick={() => changePage(page - 1)}>이전</button><span>{page + 1} / {data.totalPages}</span>
        <button className="room-list__page" disabled={loading || page >= data.totalPages - 1} onClick={() => changePage(page + 1)}>다음</button>
      </nav>}
    </main>
  )
}
