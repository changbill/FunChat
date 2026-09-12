const MessageInput = ({ inputMessage, setInputMessage, sendMessage, disabled = false }) => {
  const onKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing && !disabled) {
      e.preventDefault()
      sendMessage()
    }
  }

  const canSend = !disabled && Boolean(inputMessage?.trim())

  return (
    <div className="chat-room__form">
      <input
        type="text"
        className="chat-room__input"
        placeholder="메시지를 입력하세요"
        value={inputMessage}
        onChange={(e) => setInputMessage(e.target.value)}
        onKeyDown={onKeyDown}
        autoComplete="off"
        aria-label="메시지 입력"
      />
      <button
        type="button"
        className="chat-room__send"
        onClick={sendMessage}
        disabled={!canSend}
      >
        전송
      </button>
    </div>
  )
}

export default MessageInput
