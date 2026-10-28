package com.youlai.boot.support.realtime.protocol;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;

/**
 * 实时消息的 JSON 编解码与字段校验
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
public class RealtimeMessageCodec {
    public static final int MAX_MESSAGE_BYTES = 16_384;
    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 将服务端消息编码为 JSON 文本
     */
    public String encode(RealtimeServerMessage message) {
        String text = mapper.writeValueAsString(message);
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new IllegalArgumentException("Message too large");
        }
        return text;
    }

    /**
     * 校验客户端心跳响应并返回对应的 ping ID
     */
    public String decodePong(String text) {
        // 限制 UTF-8 字节数，中文和表情不能按字符数估算帧大小
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new IllegalArgumentException("Message too large");
        }
        JsonNode root = mapper.readTree(text);
        if (root.isObject() && root.path("version").isIntegralNumber() && root.path("version").asLong(-1) == 1
                && "heartbeat.pong".equals(root.path("type").asText())
                && root.path("payload").isObject() && isText(root.path("payload").path("pingId"))) {
            return root.path("payload").path("pingId").asText();
        }
        throw new IllegalArgumentException("Unsupported message");
    }

    /**
     * 校验文本长度与首尾空白
     */
    private boolean isText(JsonNode value) {
        return value.isString() && !value.asText().isBlank() && value.asText().length() <= 128
                && value.asText().equals(value.asText().trim());
    }
}
