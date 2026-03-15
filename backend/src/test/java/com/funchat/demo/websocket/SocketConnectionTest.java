package com.funchat.demo.websocket;

import com.funchat.demo.TestContainerTest;
import com.funchat.demo.chat.domain.ChatMessageRepository;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SocketConnectionTest extends TestContainerTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private ChatMessageRepository chatMessageRepository; // 자동으로 기존 빈을 대체

    private WebSocketStompClient stompClient;
    private WebSocketStompClient stompClient2;
    private final String URL = "ws://localhost:";

    @BeforeEach
    void setup() {
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        this.stompClient = new WebSocketStompClient(new SockJsClient(transports));
        this.stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        // 두 번째 클라이언트

        List<Transport> transports2 = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        this.stompClient2 = new WebSocketStompClient(new SockJsClient(transports2));
        this.stompClient2.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    @DisplayName("웹소켓 연결 테스트")
    void testWebSocketConnection() throws Exception {
        String wsUrl = URL + port + "/ws";
        // 보통 변수는 메모리보다 빠른 CPU 캐시(L1,L2)에 값을 저장하는데 자신의 캐시만 변경하면 다른 쓰레드에서 확인 불가.
        // volatile: 변수를 CPU 캐시말고 메모리에 저장하라는 키워드(가시성 보장)
        AtomicBoolean connected = new AtomicBoolean(false);

        // 이벤트 리스너
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            // CONNECTED 프레임 수신하자마자 실행
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println(">>>> [TEST] WebSocket 연결 성공");
                connected.set(true);
            }
        };

        // connectAsync -> 논블로킹 비동기 작업으로 CompletableFuture 반환.
        // 비동기 작업 완료할때까지 최대 5초간 대기.
        StompSession session = stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);
        // session 객체 만들어지자마자 메인 스레드는 넘어가서 아래의 connected.get()에서 오류를 일으킬 수 있다.
        Thread.sleep(500);

        assertTrue(connected.get());
        assertTrue(session.isConnected());

        session.disconnect();
    }

    @Test
    @DisplayName("채팅방 구독 테스트")
    void testChatRoomSubscription() throws Exception {
        BlockingQueue<String> subscribeQueue = new LinkedBlockingDeque<>();
        String wsUrl = URL + port + "/ws";
        String roomId = "1";

        StompSession session = getStompSession(stompClient, wsUrl, roomId, subscribeQueue);

        Thread.sleep(1000);

        Map<String, String> message = new HashMap<>();
        message.put("message", "테스트 메시지");
        session.send("/pub/send/" + roomId, message);

        String receivedMessage = subscribeQueue.poll(10, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).contains("테스트 메시지");

        session.disconnect();
    }

    @Test
    @DisplayName("여러 클라이언트 동시 연결 테스트")
    void testMultipleClientsConnection() throws Exception {
        String wsUrl = URL + port + "/ws";
        String roomId = "2";

        BlockingQueue<String> queue1 = new LinkedBlockingDeque<>();
        BlockingQueue<String> queue2 = new LinkedBlockingDeque<>();

        StompSession session1 = getStompSession(stompClient, wsUrl, roomId, queue1);
        StompSession session2 = getStompSession(stompClient2, wsUrl, roomId, queue2);

        Thread.sleep(1000);

        // 메시지 전송
        Map<String, String> message = new HashMap<>();
        message.put("message", "다중 클라이언트 테스트");
        session1.send("/pub/send/" + roomId, message);

        // 두 클라이언트 모두 메시지를 받았는지 확인
        String received1 = queue1.poll(10, TimeUnit.SECONDS);
        String received2 = queue2.poll(10, TimeUnit.SECONDS);

        assertThat(received1).isNotNull();
        assertThat(received2).isNotNull();
        assertThat(received1).contains("다중 클라이언트 테스트");
        assertThat(received2).contains("다중 클라이언트 테스트");

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("연속 메시지 전송 테스트")
    void testMultipleMessagesSend() throws Exception {
        BlockingQueue<String> subscribeQueue = new LinkedBlockingDeque<>();
        String wsUrl = URL + port + "/ws";
        String roomId = "3";

        StompSession session = getStompSession(stompClient, wsUrl, roomId, subscribeQueue);        Thread.sleep(1000);

        // 여러 메시지 전송
        Map<String, String> message = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            message.put("message", "다중 클라이언트 테스트" + i);
            session.send("/pub/send/" + roomId, message);
            Thread.sleep(100);
        }

        // 5개의 메시지가 모두 수신되었는지 확인
        int receivedCount = 0;
        while (receivedCount < 5) {
            String received = subscribeQueue.poll(5, TimeUnit.SECONDS);
            if (received != null) {
                receivedCount++;
                System.out.println(">>>> [TEST] 수신된 메시지 " + receivedCount + ": " + received);
            } else {
                break;
            }
        }

        assertThat(receivedCount).isEqualTo(5);
        session.disconnect();
    }

    @Test
    @DisplayName("연결 해제 테스트")
    void testDisconnection() throws Exception {
        String wsUrl = URL + port + "/ws";

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                // 연결 상태 확인을 위해 기본 구현을 유지
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // 테스트에서는 메시지 payload가 필요 없으므로 무시
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                fail("Transport error: " + exception.getMessage());
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                fail("STOMP exception: " + exception.getMessage());
            }
        }).get(5, TimeUnit.SECONDS);

        assertTrue(session.isConnected());

        session.disconnect();
        Thread.sleep(500);

        assertFalse(session.isConnected());
    }

    private @NonNull StompSession getStompSession(WebSocketStompClient client, String wsUrl, String roomId, BlockingQueue<String> queue2) throws InterruptedException, ExecutionException, TimeoutException {
        StompSession session = client.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/sub/chat/" + roomId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        Map<String, Object> body = (Map<String, Object>) payload;
                        queue2.add(body.get("message").toString());
                    }
                });
            }
        }).get(5, TimeUnit.SECONDS);
        return session;
    }
}
