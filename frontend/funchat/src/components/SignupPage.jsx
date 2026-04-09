import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import '../css/LoginPage.css'
import { clearAuth } from '../utils/auth'

const API_BASE = ''

const SignupPage = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()

    try {
      setLoading(true)
      setError(null)
      const res = await fetch(`${API_BASE}/api/auth/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, nickname }),
      })

      const data = await res.json()
      if (!res.ok || data?.code !== 200) {
        throw new Error(data?.message ?? '회원가입에 실패했습니다.')
      }

      // 이전 계정 JWT가 남아 있으면 LoginPage의 resolveAuthSession이 만료 전 토큰으로 / 리다이렉트 → DB에 없는 sub로 API 호출 → USER_NOT_FOUND
      clearAuth()
      navigate('/login')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="login-page">
      <form className="login-page__form" onSubmit={handleSubmit}>
        <h1 className="login-page__title">FunChat 회원가입</h1>
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
        <label className="login-page__label" htmlFor="nickname">
          닉네임
        </label>
        <input
          id="nickname"
          type="text"
          className="login-page__input"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          required
        />
        {error && <p className="login-page__error">{error}</p>}
        <button type="submit" className="login-page__button" disabled={loading}>
          {loading ? '가입 중...' : '회원가입'}
        </button>
        <p className="login-page__helper">
          이미 계정이 있나요? <Link to="/login">로그인</Link>
        </p>
      </form>
    </section>
  )
}

export default SignupPage
