import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { clearAuth } from '../utils/auth'
import { API_BASE, authHeaderRecord } from '../utils/http'

const LogoutButton = ({ className = '', beforeLogout }) => {
  const [loggingOut, setLoggingOut] = useState(false)
  const navigate = useNavigate()

  const handleLogout = async () => {
    if (loggingOut) return

    setLoggingOut(true)
    try {
      if (beforeLogout) {
        await beforeLogout()
      }

      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: authHeaderRecord(),
      })
    } catch {
      /* 로그아웃은 클라이언트 세션 제거를 우선한다. */
    } finally {
      clearAuth()
      navigate('/login', { replace: true })
    }
  }

  return (
    <button
      type="button"
      className={className}
      onClick={handleLogout}
      disabled={loggingOut}
    >
      {loggingOut ? '로그아웃 중...' : '로그아웃'}
    </button>
  )
}

export default LogoutButton
