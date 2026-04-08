/**
 * 채팅 UI 공통 모델 (GET /api/chat/messages 와 STOMP /sub/chat 동일 필드)
 *
 * @typedef {Object} ChatMessageView
 * @property {string} [messageId] Mongo 저장분만 존재
 * @property {number} roomId
 * @property {number} senderId
 * @property {string} senderNickname
 * @property {string} content
 * @property {string} [createdAt]
 * @property {string} [type] JOIN, TEXT 등
 */

const toNum = (v, fallback = 0) => {
  if (v == null || v === '') return fallback
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

/**
 * API·브로커가 주는 임의 객체를 화면용 한 형태로 맞춘다.
 * (Redis 스트림은 숫자 필드가 문자열로 올 수 있음)
 *
 * @param {unknown} raw
 * @returns {ChatMessageView | null}
 */
export function normalizeChatMessage(raw) {
  if (raw == null || typeof raw !== 'object' || Array.isArray(raw)) {
    return null
  }
  const o = /** @type {Record<string, unknown>} */ (raw)
  const messageId = o.messageId
  return {
    ...(messageId != null ? { messageId: String(messageId) } : {}),
    roomId: toNum(o.roomId),
    senderId: toNum(o.senderId),
    senderNickname: String(o.senderNickname ?? '알 수 없음'),
    content: String(o.content ?? ''),
    ...(o.createdAt != null ? { createdAt: String(o.createdAt) } : {}),
    ...(o.type != null ? { type: String(o.type) } : {}),
  }
}

/**
 * STOMP MESSAGE body → JSON 객체. (stompjs는 보통 UTF-8 문자열)
 *
 * @param {unknown} body
 * @returns {Record<string, unknown> | null}
 */
export function parseStompBody(body) {
  if (body == null) return null

  if (typeof body === 'object') {
    if (body instanceof ArrayBuffer) {
      body = new TextDecoder().decode(body)
    } else if (body instanceof Uint8Array) {
      body = new TextDecoder().decode(body)
    } else {
      return /** @type {Record<string, unknown>} */ (body)
    }
  }

  const text = String(body).trim()
  if (!text) return null

  try {
    const parsed = JSON.parse(text)
    if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return /** @type {Record<string, unknown>} */ (parsed)
    }
    if (typeof parsed === 'string') {
      return { content: parsed }
    }
    return { content: String(parsed) }
  } catch {
    return { content: text }
  }
}
