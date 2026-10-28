package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.security.RealtimeTicketService.TicketGrant;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Set;

/**
 * 可变连接状态由本对象的监视器保护，锁内不执行网络或存储操作
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
final class RealtimeSession {
    final WebSocketSession transport;
    final Long userId;
    final String instanceKey;
    final RealtimeSessionManager.Reservation reservation;
    final ArrayDeque<RealtimeMessageSender.Frame> queue = new ArrayDeque<>();
    private String accessToken;
    final Set<String> topics;
    final Instant authExpiresAt;
    // 间隔判断使用单调时钟纳秒；协议中的绝对到期时间使用 Instant
    long lastVerifiedAt = System.nanoTime();
    long lastPongAt = System.nanoTime();
    long nextPingAt = lastPongAt + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
    String pendingPingId;
    long nextVerifyAt = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
            + java.util.concurrent.ThreadLocalRandom.current().nextLong(java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
    // 预算包含排队和正在写出的帧，由发送者分别结算
    int bufferedBytes;
    int bufferedMessages;
    int inboundMessages;
    long inboundWindow = System.nanoTime();
    boolean draining;
    // closed 停止业务收发；terminated 表示底层连接结束，可释放配额
    boolean closed;
    boolean terminated;
    boolean verifying;
    boolean closeRunning;
    int closeAttempts;
    CloseStatus closeStatus = CloseStatus.NORMAL;

    RealtimeSession(WebSocketSession transport, TicketGrant grant,
                    RealtimeSessionManager.Reservation reservation, Instant authExpiresAt) {
        this.transport = transport;
        this.userId = grant.userId();
        this.instanceKey = grant.userId() + ":" + grant.clientInstanceId();
        this.accessToken = grant.accessToken();
        this.topics = grant.topics();
        this.reservation = reservation;
        this.authExpiresAt = authExpiresAt;
    }

    synchronized String accessToken() {
        return accessToken;
    }
    /**
     * 清除连接持有的认证凭证
     */
    synchronized void forgetCredential() {
        accessToken = null;
    }

    @Override
    public String toString() {
        return "RealtimeSession[transport=" + transport.getId() + "]";
    }
}
