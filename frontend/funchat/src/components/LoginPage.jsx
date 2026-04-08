import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import '../css/LoginPage.css'
import { saveAuth, resolveAuthSession } from '../utils/auth'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

const LoginPage = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    resolveAuthSession().then((ok) => {
      if (!cancelled && ok) navigate('/', { replace: true })
    })
    return () => {
      cancelled = true
    }
  }, [navigate])

  const handleSubmit = async (e) => {
    e.preventDefault()

    try {
      setLoading(true)
      setError(null)
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })

      const data = await res.json()
      if (!res.ok || !data?.body?.accessToken) {
        throw new Error(data?.message ?? '로그인에 실패했습니다.')
      }

      saveAuth(data.body)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="login-page">
      <form className="login-page__form" onSubmit={handleSubmit}>
        <h1 className="login-page__title">FunChat 로그인</h1>
        <label className="login-page__label" htmlFor="email">
          이메일
        </label>
        <input
          id="email"
          type="email"
          className="login-page__input"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <label className="login-page__label" htmlFor="password">
          비밀번호
        </label>
        <input
          id="password"
          type="password"
          className="login-page__input"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="login-page__error">{error}</p>}
        <button type="submit" className="login-page__button" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
        <p className="login-page__helper">
          계정이 없나요? <Link to="/signup">회원가입</Link>
        </p>
      </form>
    </section>
  )
}

export default LoginPage
