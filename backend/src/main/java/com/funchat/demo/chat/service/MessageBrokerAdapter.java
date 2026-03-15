package com.funchat.demo.chat.service;

// messagebroker에 대한 발행과 구독
public interface MessageBrokerAdapter {
    /**
     * @param topic 메시지를 보낼 스트림 키 (예: "chat-stream")
     * @param message 전송할 데이터 객체
     */
    void publish(String topic, Object message);
    /**
     * @param topic 구독할 스트림 키
     * @param consumerGroup 서버 인스턴스 간 분산 처리를 위한 그룹명
     * @param handler 메시지 수신 시 실행할 로직
     */
    void subscribe(String topic, String consumerGroup, MessageHandler handler);
}