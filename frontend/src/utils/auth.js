import { API_BASE } from './apiBase'

const ACCESS_TOKEN_KEY = import.meta.env.VITE_ACCESS_TOKEN_KEY ?? 'accessToken'
const REFRESH_TOKEN_KEY =
  import.meta.env.VITE_REFRESH_TOKEN_KEY ?? 'refreshToken'
const NICKNAME_KEY = import.meta.env.VITE_NICKNAME_KEY ?? 'nickname'

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY)

export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY)

export const getNickname = () => localStorage.getItem(NICKNAME_KEY) ?? ''

function decodeJwtPayload(token) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch {
    return null
  }
}

/** 만료·파싱 불가·exp 없음 → true (유효하지 않음) */
export const isAccessTokenExpired = (token) => {
  if (!token || typeof token !== 'string' || !token.trim()) return true
  const payload = decodeJwtPayload(token)
  if (!payload?.exp) return true
  return payload.exp * 1000 <= Date.now()
}

/** 동기: 저장된 문자열뿐 아니라 JWT exp까지 통과한 액세스 토큰이 있을 때만 true */
export const hasValidAccessToken = () => {
  const t = getAccessToken()
  return Boolean(t?.trim()) && !isAccessTokenExpired(t)
}

export const isLoggedIn = () => hasValidAccessToken()

export const saveAuth = ({ accessToken, refreshToken, nickname }) => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  localStorage.setItem(NICKNAME_KEY, nickname ?? '')
}

export const clearAuth = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(NICKNAME_KEY)
}

/**
 * 서버 기준 세션 복구: 액세스 유효 → true, 만료 시 리프레시로 재발급 시도 → 성공 시 true, 실패 시 clearAuth 후 false
 */
export async function resolveAuthSession() {
  if (hasValidAccessToken()) {
    return true
  }

  const refresh = getRefreshToken()
  if (!refresh?.trim()) {
    if (getAccessToken()) clearAuth()
    return false
  }

  try {
    const res = await fetch(`${API_BASE}/api/auth/reissue`, {
      method: 'POST',
      headers: {
        'Authorization-Refresh': `Bearer ${refresh}`,
      },
      credentials: 'include',
    })
    const data = await res.json()
    if (!res.ok || !data?.body?.accessToken) {
      clearAuth()
      return false
    }
    const body = data.body
    saveAuth({
      accessToken: body.accessToken,
      refreshToken: body.refreshToken,
      nickname: body.nickname ?? getNickname(),
    })
    return true
  } catch {
    clearAuth()
    return false
  }
}
