import { getAccessToken } from './auth'
import { API_BASE } from './apiBase'

export { API_BASE }

/** Authorization 헤더만(토큰 없으면 빈 객체) */
export const authHeaderRecord = () => {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/**
 * fetch 응답을 JSON(가능하면) / 아니면 text 를 message로 갖는 객체로 정규화
 * (Login/Signup에서 반복되던 파싱 로직 공용화)
 */
export async function readJsonOrTextPayload(res) {
  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return await res.json()
  }
  return { message: (await res.text()) || '서버 응답을 해석하지 못했습니다.' }
}
