package com.youlai.boot.platform.websocket.controller;

import com.youlai.boot.platform.websocket.dto.TextMessage;
import com.youlai.boot.platform.websocket.publisher.WebSocketPublisher;
import com.youlai.boot.platform.websocket.topic.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * WebSocket 测试用例控制层
 * <p>
 * 包含点对点/广播发送消息
 *
 * @author Ray.Hao
 * @since 2.3.0
 */
@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebsocketController {

    private final WebSocketPublisher webSocketPublisher;


    /**
     * 广播发送消息
     *
     * @param message 消息内容
     */
    @MessageMapping("/sendToAll")
    @SendTo("/topic/notice")
    public String sendToAll(String message) {
        return "服务端通知: " + message;
    }

    /**
     * 点对点发送消息
     * <p>
     * 模拟 张三 给 李四 发送消息场景
     *
     * @param principal 当前用户
     * @param username  接收消息的用户
     * @param message   消息内容
     */
    @MessageMapping("/sendToUser/{username}")
    public void sendToUser(Principal principal, @DestinationVariable String username, String message) {
        // 发送人
        String sender = principal.getName();
        // 接收人
        String receiver = username;

        log.info("发送人:{}; 接收人:{}", sender, receiver);
        // 发送消息给指定用户，拼接后路径 /user/{receiver}/queue/greeting
        webSocketPublisher.publishToUser(receiver, WebSocketTopics.USER_QUEUE_GREETING, new TextMessage(sender, message, System.currentTimeMillis()));
    }

}
