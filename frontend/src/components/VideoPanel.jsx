import { useCallback, useEffect, useRef, useState } from 'react'
import { LiveKitRoom, GridLayout, ParticipantTile, RoomAudioRenderer, StartAudio, TrackToggle, useTracks, useConnectionState, useParticipants } from '@livekit/components-react'
import { Track } from 'livekit-client'
import { roomRequest } from '../utils/roomApi'
import '@livekit/components-styles'
import '../css/VideoRoom.css'

function CallContents({ onLeave, onError }) {
  const tracks = useTracks([{ source: Track.Source.Camera, withPlaceholder: true }, { source: Track.Source.ScreenShare, withPlaceholder: false }])
  const state = useConnectionState()
  const participants = useParticipants()
  const labels = { connected: '연결됨', connecting: '연결 중…', reconnecting: '다시 연결 중…', disconnected: '연결 종료', 'signalReconnecting': '다시 연결 중…' }
  return <>
    <div className="video-panel__status" role="status"><span className="video-panel__dot" />{labels[state] ?? '연결 확인 중…'} · {participants.length}명 참가</div>
    <GridLayout tracks={tracks} className="video-panel__grid"><ParticipantTile /></GridLayout>
    <RoomAudioRenderer />
    <StartAudio label="상대방 소리 듣기" />
    <div className="video-panel__controls">
      <TrackToggle source={Track.Source.Microphone} onDeviceError={onError}>마이크</TrackToggle>
      <TrackToggle source={Track.Source.Camera} onDeviceError={onError}>카메라</TrackToggle>
      <TrackToggle source={Track.Source.ScreenShare} onDeviceError={onError}>화면 공유</TrackToggle>
      <button className="video-panel__leave" onClick={onLeave}>통화 나가기</button>
    </div>
  </>
}

export default function VideoPanel({ roomId }) {
  const [credentials, setCredentials] = useState(null)
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const request = useRef(null)
  useEffect(() => () => request.current?.abort(), [])

  const join = async () => {
    if (request.current) return
    if (!window.isSecureContext) { setError('영상통화는 HTTPS 또는 localhost에서 사용할 수 있습니다.'); return }
    const controller = new AbortController()
    request.current = controller
    setJoining(true); setError(''); setNotice('')
    try {
      const result = await roomRequest(`/${roomId}/video/token`, { method: 'POST', signal: controller.signal })
      if (controller.signal.aborted) return
      if (!result?.token || !result?.livekitUrl) throw new Error('영상통화 연결 정보를 받지 못했습니다.')
      setCredentials(result)
    } catch (err) {
      if (!controller.signal.aborted) setError(err.message)
    } finally {
      if (!controller.signal.aborted) setJoining(false)
      request.current = null
    }
  }
  const deviceError = useCallback(() => setError('장치를 켜지 못했습니다. 브라우저의 카메라·마이크 권한과 다른 앱의 장치 사용 여부를 확인해 주세요.'), [])
  const connectionError = useCallback(() => { setError('영상 서버에 연결하지 못했습니다. 잠시 후 다시 참가해 주세요.'); setCredentials(null) }, [])
  const disconnected = useCallback(() => { setCredentials(null); setNotice('통화 연결이 종료되었습니다. 다시 참가할 수 있습니다.') }, [])
  const leaveCall = useCallback(() => { setCredentials(null); setNotice('통화에서 나왔습니다. 채팅은 계속할 수 있어요.') }, [])

  return <section className="video-panel" aria-label="영상통화">
    {error && <p className="video-panel__error" role="alert">{error}</p>}
    {credentials ? <LiveKitRoom token={credentials.token} serverUrl={credentials.livekitUrl} connect audio={false} video={false}
      onError={connectionError}
      onMediaDeviceFailure={deviceError}
      onDisconnected={disconnected}
      data-lk-theme="default">
      <CallContents onLeave={leaveCall} onError={deviceError} />
    </LiveKitRoom> : <div className="video-panel__welcome">
      <span className="video-panel__symbol" aria-hidden="true">◉</span>
      <p className="room-list__eyebrow">함께하는 영상 대화</p><h2>얼굴 보며 이야기해요</h2>
      <p>카메라와 마이크는 꺼진 상태로 참가합니다.<br />준비되면 원하는 장치를 켜 주세요.</p>
      {notice && <p role="status">{notice}</p>}
      <button className="room-list__enter" disabled={joining} onClick={join}>{joining ? '참가 준비 중…' : '영상통화 참가'}</button>
    </div>}
  </section>
}
