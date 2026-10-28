package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.protocol.RealtimeMessageCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户端协议的最小字段与输入边界
 *
 * @since 4.13.0
 */
class RealtimeMessageCodecTest {
    private final RealtimeMessageCodec codec = new RealtimeMessageCodec();

    @Test
    void acceptsPongWithoutMessageIdOrTimestamp() {
        var pingId = codec.decodePong("""
                {"version":1,"type":"heartbeat.pong","payload":{"pingId":"ping-1"}}
                """);
        assertThat(pingId).isEqualTo("ping-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null",
            "[]",
            "{\"version\":2,\"type\":\"heartbeat.pong\",\"payload\":{\"pingId\":\"p\"}}",
            "{\"version\":1,\"type\":\"heartbeat.pong\",\"payload\":{}}",
            "{\"version\":1,\"type\":\"heartbeat.pong\",\"payload\":{\"pingId\":\" \"}}",
            "{\"version\":1,\"type\":\"session.revoked\",\"payload\":{}}",
            "{\"version\":1,\"type\":\"subscription.replace\",\"payload\":{\"topics\":[\"notice\",\"dict\"]}}"
    })
    void rejectsInvalidMessages(String text) {
        assertThatThrownBy(() -> codec.decodePong(text)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limitsUtf8BytesInsteadOfCharacterCount() {
        String text = "{\"version\":1,\"type\":\"heartbeat.pong\",\"payload\":{\"pingId\":\"p\"},\"extra\":\""
                + "字".repeat(6_000) + "\"}";
        assertThat(text.length()).isLessThan(RealtimeMessageCodec.MAX_MESSAGE_BYTES);
        assertThatThrownBy(() -> codec.decodePong(text)).hasMessage("Message too large");
    }
}
