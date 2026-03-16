const MessageList = ({ messages }) => {
  return (
    <div className="message-list">
      <ul>
        {messages.map((msg, idx) => (
          <li key={idx}>{msg}</li>
        ))}
      </ul>
    </div>
  )
}

export default MessageList
