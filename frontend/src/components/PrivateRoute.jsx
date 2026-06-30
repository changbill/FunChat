import { useEffect, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { resolveAuthSession } from '../utils/auth'

const PrivateRoute = ({ children }) => {
  const [ready, setReady] = useState(false)
  const [authed, setAuthed] = useState(false)
  const location = useLocation()

  useEffect(() => {
    let cancelled = false
    resolveAuthSession().then((ok) => {
      if (!cancelled) {
        setAuthed(ok)
        setReady(true)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  if (!ready) {
    return (
      <div
        style={{
          minHeight: '40vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#9ca3af',
          fontSize: '0.875rem',
        }}
      >
        세션 확인 중…
      </div>
    )
  }

  if (!authed) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return children
}

export default PrivateRoute
