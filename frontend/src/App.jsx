import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import LoginPage from './components/LoginPage'
import SignupPage from './components/SignupPage'
import PrivateRoute from './components/PrivateRoute'
import RoomList from './components/RoomList'
import ChatContainer from './components/ChatContainer'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <RoomList />
            </PrivateRoute>
          }
        />
        <Route
          path="/room/:roomId"
          element={
            <PrivateRoute>
              <ChatContainer />
            </PrivateRoute>
          }
        />
      </Routes>
    </Router>
  )
}

export default App
