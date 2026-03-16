import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client 등에서 참조하는 global 객체를 window로 매핑
    global: 'window',
  },
})
