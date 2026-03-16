import { useState, useEffect } from 'react'
import MessageInput from './MessageInput'
import MessageList from './MessageList'
import SockJS from 'sockjs-client'
import { over } from 'stompjs'

const ChatContainer = () => {
  const [stompClient, setStompClient] = useState(null)
  const [receivedMessages, setReceivedMessages] = useState([])
  const [inputMessage, setInputMessage] = useState('')

  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws')
    const client = over(socket)

    console.log('웹소켓 연결 시도')

    client.connect(
      {},
      () => {
        console.log('웹소켓 연결 성공')
        setStompClient(client)
        client.subscribe('/sub/messages', (message) => {
          console.log(message)
          setReceivedMessages((prev) => [...prev, message.body])
        })
      },
      (error) => {
        console.log('웹소켓 연결 실패', error)
      },
    )
  }, [])

  const sendMessage = () => {
    if (stompClient && inputMessage.trim()) {
      stompClient.send('/pub/send', {}, inputMessage)
      setInputMessage('')
    }
  }

  const endConnection = () => {
    if (stompClient) {
      stompClient.disconnect(() => {
        console.log('웹소켓 연결 종료')
        setStompClient(null)
      })
    }
  }

  return (
    <div>
      <h1>채팅방</h1>
      <div>
        <MessageInput
          inputMessage={inputMessage}
          setInputMessage={setInputMessage}
          sendMessage={sendMessage}
        />
        <button onClick={endConnection}>방 나가기기</button>
      </div>

      <MessageList messages={receivedMessages} />
    </div>
  )
}

export default ChatContainer
