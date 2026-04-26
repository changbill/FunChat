package com.funchat.demo;

import com.funchat.demo.chat.service.ChatFanoutBroker;
import com.funchat.demo.chat.service.ChatPersistBroker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

	@MockitoBean
	private ChatPersistBroker chatPersistBroker;

	@MockitoBean
	private ChatFanoutBroker chatFanoutBroker;

	@Test
	void contextLoads() {
	}

}
