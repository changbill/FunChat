package com.funchat.demo.websocket;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// 포트번호가 랜덤이 되며, @LocalServerPort를 통해 포트번호를 불러올수 있다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SocketConnectionTest {

    @LocalServerPort
    private int port;

}