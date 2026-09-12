import { API_BASE, authHeaderRecord, readJsonOrTextPayload } from './http'

export async function roomRequest(path, { method = 'GET', body, signal } = {}) {
  const response = await fetch(`${API_BASE}/api/rooms${path}`, {
    method,
    credentials: 'include',
    headers: { ...authHeaderRecord(), ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal,
  })
  const payload = await readJsonOrTextPayload(response)
  if (!response.ok) {
    const error = new Error(payload?.message || '요청을 처리하지 못했습니다. 다시 시도해 주세요.')
    error.status = response.status
    throw error
  }
  return payload.body
}
