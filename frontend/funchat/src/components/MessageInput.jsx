const MessageInput = ({ inputMessage, setInputMessage, sendMessage }) => {
  return (
    <div>
      <input
        type="text"
        placeholder="메시지"
        value={inputMessage}
        onChange={(e) => setInputMessage(e.target.value)}
      ></input>
      <button onClick={sendMessage}>메시지 전송</button>
    </div>
  )
}

export default MessageInput
