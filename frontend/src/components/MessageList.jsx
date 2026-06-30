const MessageList = ({ messages }) => {
  if (!messages?.length) {
    return (
      <p className="chat-room__empty">메시지가 없습니다. 대화를 시작해 보세요.</p>
    )
  }

  return (
    <ul className="chat-room__list">
      {messages.map((msg, idx) => (
        <li
          key={msg.messageId ?? `${idx}-${msg.senderId}-${msg.createdAt ?? ''}`}
          className="chat-room__msg"
        >
          <p className="chat-room__msg-meta">{msg.senderNickname ?? '알 수 없음'}</p>
          <p className="chat-room__msg-text">{msg.content ?? ''}</p>
        </li>
      ))}
    </ul>
  )
}

export default MessageList
