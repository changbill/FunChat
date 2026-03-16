import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import RoomList from './components/RoomList'
import ChatContainer from './components/ChatContainer'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<RoomList />} />
        <Route path="/room/:roomId" element={<ChatContainer />} />
      </Routes>
    </Router>
  )
}

export default App
