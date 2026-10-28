package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.protocol.RealtimeMessageCodec;
import com.youlai.boot.support.realtime.security.RealtimeCredentialVerifier;
import com.youlai.boot.support.realtime.security.RealtimeCredentialVerifier.VerifiedIdentity;
import com.youlai.boot.support.realtime.security.RealtimeTicketService.TicketGrant;
import com.youlai.boot.support.realtime.security.RealtimeTicketService.VerifiedTicket;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 连接期限、权限复核、在线统计与配额生命周期
 *
 * @since 4.13.0
 */
class RealtimeSessionManagerTest {
    private final RealtimeCredentialVerifier verifier = mock(RealtimeCredentialVerifier.class);
    private final RealtimeMessageCodec codec = new RealtimeMessageCodec();
    private final RealtimeMessageSender sender = new RealtimeMessageSender(Runnable::run);
    private final RealtimeSessionManager sessions = new RealtimeSessionManager(sender, codec, verifier, Runnable::run);
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void registrationUsesVerifiedTicketAndSendsMinimalReady() throws Exception {
        List<String> messages = new ArrayList<>();
        var transport = transport("socket-1", messages);
        Instant expiresAt = Instant.now().plusSeconds(3_600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        sessions.register(transport, ticket(1L, "client-1", expiresAt, Set.of("notice")), sessions.reserve());

        var ready = mapper.readTree(messages.get(0));
        assertThat(ready.path("type").asText()).isEqualTo("connection.ready");
        assertThat(ready.path("payload")).isEqualTo(mapper.readTree("{\"heartbeatTimeoutMs\":60000}"));
        assertThat(ready.has("occurredAt")).isFalse();
        assertThat(state("socket-1").authExpiresAt).isEqualTo(expiresAt);
        verifyNoInteractions(verifier);
    }

    @Test
    void presenceChangesWhenUserCountChangesWithSameConnectionCount() throws Exception {
        List<String> messages = new ArrayList<>();
        var first = transport("first", messages);
        var second = transport("second", new ArrayList<>());
        sessions.register(first, ticket(1L, "a", Instant.now().plusSeconds(3_600), Set.of("notice", "presence")), sessions.reserve());
        sessions.register(second, ticket(1L, "b", Instant.now().plusSeconds(3_600), Set.of("notice")), sessions.reserve());
        when(verifier.verify("token-1")).thenReturn(new VerifiedIdentity(1L, true, Instant.now().plusSeconds(3_600)));
        sessions.maintain();
        messages.clear();

        sessions.terminated(second);
        var replacement = transport("replacement", new ArrayList<>());
        sessions.register(replacement, ticket(2L, "c", Instant.now().plusSeconds(3_600), Set.of("notice")), sessions.reserve());
        ReflectionTestUtils.setField(sessions, "lastPresenceAt", System.nanoTime() - TimeUnit.SECONDS.toNanos(6));
        sessions.maintain();

        var snapshots = messages.stream().map(mapper::readTree)
                .filter(message -> "presence.snapshot".equals(message.path("type").asText())).toList();
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).path("payload").path("onlineUsers").asInt()).isEqualTo(2);
        assertThat(snapshots.get(0).path("payload").path("onlineConnections").asInt()).isEqualTo(2);
    }

    @Test
    void periodicVerificationStillClosesRevokedCredentials() throws Exception {
        var transport = transport("socket-1", new ArrayList<>());
        sessions.register(transport, ticket(1L, "a", Instant.now().plusSeconds(3_600), Set.of("notice")), sessions.reserve());
        state("socket-1").nextVerifyAt = 0;
        when(verifier.verify("token-1")).thenThrow(new BadCredentialsException("revoked"));

        sessions.maintain();

        verify(transport).close(new CloseStatus(4001, "credential revoked"));
        assertThat(sessions.presence().onlineConnections()).isZero();
        assertThat(state("socket-1").accessToken()).isNull();
    }

    @Test
    void periodicVerificationStillChecksPresencePermission() throws Exception {
        var transport = transport("socket-1", new ArrayList<>());
        sessions.register(transport, ticket(1L, "a", Instant.now().plusSeconds(3_600), Set.of("notice", "presence")), sessions.reserve());
        when(verifier.verify("token-1")).thenReturn(new VerifiedIdentity(1L, false, Instant.now().plusSeconds(3_600)));
        state("socket-1").nextVerifyAt = 0;

        sessions.maintain();

        verify(transport).close(new CloseStatus(4001, "credential revoked"));
    }

    @Test
    void expiredCredentialsNeverReceiveReadyAndReleaseQuotaOnlyAfterTermination() throws Exception {
        List<String> messages = new ArrayList<>();
        var transport = transport("socket-1", messages);
        var reservation = sessions.reserve();
        sessions.register(transport, ticket(1L, "a", Instant.now().minusSeconds(1), Set.of("notice")), reservation);
        List<RealtimeSessionManager.Reservation> otherReservations = new ArrayList<>();
        for (int index = 0; index < 499; index++) {
            otherReservations.add(sessions.reserve());
        }

        assertThat(messages).isEmpty();
        assertThat(sessions.reserve()).isNull();
        sessions.terminated(transport);
        sessions.terminated(transport);
        reservation.close();
        var released = sessions.reserve();
        assertThat(released).isNotNull();
        assertThat(sessions.reserve()).isNull();
        released.close();
        otherReservations.forEach(RealtimeSessionManager.Reservation::close);
    }

    @Test
    void keepsOneOutstandingPingUntilMatchingPongOrTimeout() throws Exception {
        List<String> messages = new ArrayList<>();
        var transport = transport("socket-1", messages);
        sessions.register(transport, ticket(1L, "a", Instant.now().plusSeconds(3_600), Set.of("notice")), sessions.reserve());
        RealtimeSession state = state("socket-1");
        state.nextPingAt = 0;

        sessions.maintain();
        String pingId = state.pendingPingId;
        assertThat(pingId).isNotBlank();
        sessions.maintain();
        sessions.handlePong(transport, "wrong-ping");
        assertThat(state.pendingPingId).isEqualTo(pingId);
        assertThat(messages).hasSize(2);

        sessions.handlePong(transport, pingId);
        assertThat(state.pendingPingId).isNull();
        assertThat(state.nextPingAt - state.lastPongAt).isEqualTo(TimeUnit.SECONDS.toNanos(25));
        state.lastPongAt = System.nanoTime() - TimeUnit.SECONDS.toNanos(61);
        sessions.maintain();
        verify(transport).close(new CloseStatus(4011, "heartbeat timeout"));
    }

    @Test
    void queuedAndInflightFramesShareBudgetAndReleaseItOnce() throws Exception {
        var tasks = new ArrayDeque<Runnable>();
        var queuedSender = new RealtimeMessageSender(tasks::add);
        var transport = mock(WebSocketSession.class);
        var ticket = ticket(1L, "a", Instant.now().plusSeconds(3_600), Set.of("notice"));
        var state = new RealtimeSession(transport, ticket.grant(), sessions.reserve(), ticket.credentialExpiresAt());
        var frame = RealtimeMessageSender.Frame.of("x".repeat(16_384));
        Runnable failure = () -> { throw new AssertionError("发送不应失败"); };
        assertThat(queuedSender.enqueue(state, frame, failure)).isTrue();
        assertThat(queuedSender.enqueue(state, frame, failure)).isTrue();
        assertThat(queuedSender.enqueue(state, frame, failure)).isFalse();
        assertThat(state.bufferedBytes).isEqualTo(32_768);

        doAnswer(call -> {
            state.closed = true;
            queuedSender.discardQueued(state);
            assertThat(state.bufferedBytes).isEqualTo(16_384);
            assertThat(state.bufferedMessages).isEqualTo(1);
            return null;
        }).when(transport).sendMessage(any());
        tasks.remove().run();

        assertThat(state.bufferedBytes).isZero();
        assertThat(state.bufferedMessages).isZero();
        assertThat(state.queue).isEmpty();
        state.reservation.close();
    }

    /**
     * 创建握手完成的测试票据
     */
    private VerifiedTicket ticket(long userId, String clientId, Instant expiresAt, Set<String> topics) {
        var grant = new TicketGrant("token-" + userId, userId, clientId, "web",
                topics, Instant.now().plusSeconds(30).toEpochMilli());
        return new VerifiedTicket(grant, expiresAt);
    }

    /**
     * 记录连接实际发送的文本帧
     */
    private WebSocketSession transport(String id, List<String> messages) throws Exception {
        var transport = mock(WebSocketSession.class);
        when(transport.getId()).thenReturn(id);
        doAnswer(call -> {
            messages.add(((TextMessage) call.getArgument(0)).getPayload());
            return null;
        }).when(transport).sendMessage(any());
        return transport;
    }

    /**
     * 取得连接状态以推进测试时钟，不等待真实心跳间隔
     */
    @SuppressWarnings("unchecked")
    private RealtimeSession state(String id) {
        return ((Map<String, RealtimeSession>) ReflectionTestUtils.getField(sessions, "transports")).get(id);
    }
}
