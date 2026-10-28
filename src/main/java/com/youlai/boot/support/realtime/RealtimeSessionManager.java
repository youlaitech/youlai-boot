package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.model.PresenceSnapshot;
import com.youlai.boot.support.realtime.protocol.RealtimeMessageCodec;
import com.youlai.boot.support.realtime.protocol.RealtimeServerMessage;
import com.youlai.boot.support.realtime.security.RealtimeCredentialVerifier;
import com.youlai.boot.support.realtime.security.RealtimeTicketService.VerifiedTicket;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 管理节点内连接、连接配额与授权期限
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Slf4j
@Component
public class RealtimeSessionManager {
    private static final long PONG_TIMEOUT = TimeUnit.SECONDS.toNanos(60);
    private static final long VERIFY_DEADLINE = TimeUnit.SECONDS.toNanos(30);
    // transport 保留握手与关闭中连接，active 只保留当前可投递的实例
    private final Map<String, RealtimeSession> transports = new ConcurrentHashMap<>();
    private final Map<String, RealtimeSession> active = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(500);
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicLong rejectedTasks = new AtomicLong();
    private final AtomicLong lastRejectionWarning = new AtomicLong();
    private final String nodeId = UUID.randomUUID().toString();
    private final RealtimeMessageSender sender;
    private final RealtimeMessageCodec codec;
    private final RealtimeCredentialVerifier verifier;
    private final Executor maintenance;
    private long lastPresenceAt;
    private int lastOnlineUsers = -1;
    private int lastOnlineConnections = -1;

    public RealtimeSessionManager(RealtimeMessageSender sender, RealtimeMessageCodec codec,
                                  RealtimeCredentialVerifier verifier,
                                  @Qualifier("realtimeMaintenanceExecutor") Executor maintenance) {
        this.sender = sender;
        this.codec = codec;
        this.verifier = verifier;
        this.maintenance = maintenance;
    }

    /**
     * 握手、活跃和关闭中的 transport 共用节点配额
     */
    public Reservation reserve() {
        if (stopping.get() || !slots.tryAcquire()) {
            return null;
        }
        return new Reservation();
    }

    /**
     * 连接配额预留，重复关闭只释放一次
     */
    public final class Reservation implements AutoCloseable {
        private final AtomicBoolean released = new AtomicBoolean();
        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                slots.release();
            }
        }
    }

    /**
     * 注册已验证的连接，替换同一客户端的旧实例
     */
    public void register(WebSocketSession transport, VerifiedTicket ticket, Reservation reservation) {
        RealtimeSession session = new RealtimeSession(transport, ticket.grant(), reservation, ticket.credentialExpiresAt());
        transports.put(transport.getId(), session);
        try {
            RealtimeSession previous;
            synchronized (session) {
                synchronized (active) {
                    if (stopping.get()) {
                        throw new IllegalStateException("Stopping");
                    }
                    previous = active.get(session.instanceKey);
                    long count = active.values().stream().filter(item -> item.userId.equals(session.userId)).count();
                    if (previous == null && count >= 8) {
                        throw new RejectedExecutionException("User connection limit");
                    }
                    // 先排入 ready，再向广播公开连接；旧连接关闭只能移除其自身
                    offer(session, RealtimeServerMessage.create("connection.ready", Map.of("heartbeatTimeoutMs", 60_000)));
                    if (session.closed) {
                        return;
                    }
                    active.put(session.instanceKey, session);
                }
            }
            if (previous != null) {
                requestClose(previous, 4009, "instance replaced");
            }
        } catch (Exception exception) {
            requestClose(session, 4001, "credential rejected");
        }
    }

    /**
     * 确认当前心跳并限制接收频率
     */
    public void handlePong(WebSocketSession transport, String pingId) {
        RealtimeSession session = transports.get(transport.getId());
        if (session == null) {
            return;
        }
        long now = System.nanoTime();
        synchronized (session) {
            if (session.closed) {
                return;
            }
            if (now - session.inboundWindow >= TimeUnit.SECONDS.toNanos(1)) {
                session.inboundWindow = now;
                session.inboundMessages = 0;
            }
            if (++session.inboundMessages > 20) {
                requestClose(session, 4008, "message rate exceeded");
                return;
            }
            if (pingId.equals(session.pendingPingId) && now - session.lastPongAt < PONG_TIMEOUT) {
                session.pendingPingId = null;
                session.lastPongAt = now;
                session.nextPingAt = now + TimeUnit.SECONDS.toNanos(25);
            }
        }
    }

    /**
     * 向指定用户的活跃连接投递消息
     */
    public void sendToUsers(Collection<Long> userIds, RealtimeServerMessage message) {
        Set<Long> recipients = Set.copyOf(userIds);
        for (RealtimeSession session : active.values()) {
            if (recipients.contains(session.userId)) {
                offer(session, message);
            }
        }
    }

    /**
     * 向订阅指定主题的活跃连接投递消息
     */
    public void broadcast(String topic, RealtimeServerMessage message) {
        for (RealtimeSession session : active.values()) {
            if (!session.topics.contains(topic)) {
                continue;
            }
            offer(session, message);
        }
    }

    /**
     * 检查授权期限后投递消息，超出缓冲预算时关闭连接
     */
    private void offer(RealtimeSession session, RealtimeServerMessage message) {
        if (isAuthorizationExpired(session)) {
            requestClose(session, 4012, "authorization expired");
            return;
        }
        var frame = RealtimeMessageSender.Frame.of(codec.encode(message));
        if (!sender.enqueue(session, frame, () -> requestClose(session, 4010, "write unavailable"))) {
            requestClose(session, 4010, "slow consumer");
        }
    }

    /**
     * 统计当前节点的在线用户数与连接数
     */
    public PresenceSnapshot presence() {
        var sessions = List.copyOf(active.values());
        return new PresenceSnapshot(Math.toIntExact(sessions.stream().map(item -> item.userId).distinct().count()),
                sessions.size(), nodeId, Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
    }

    /**
     * 标记连接关闭并安排底层资源清理
     */
    public void requestClose(WebSocketSession transport, int code, String reason) {
        RealtimeSession session = transports.get(transport.getId());
        if (session != null) {
            requestClose(session, code, reason);
        }
    }

    /**
     * 标记连接关闭并安排底层资源清理
     */
    private void requestClose(RealtimeSession session, int code, String reason) {
        synchronized (session) {
            if (session.closed) {
                return;
            }
            session.closed = true;
            session.closeStatus = new CloseStatus(code, reason);
        }
        synchronized (active) {
            active.remove(session.instanceKey, session);
        }
        sender.discardQueued(session);
        session.forgetCredential();
        scheduleClose(session);
    }

    /**
     * 安排底层关闭操作，失败时由维护任务有限重试
     */
    private void scheduleClose(RealtimeSession session) {
        synchronized (session) {
            if (session.terminated || session.closeRunning || session.closeAttempts >= 3) {
                return;
            }
            session.closeRunning = true;
        }
        if (!submitMaintenance(() -> {
            try {
                synchronized (session) {
                    session.closeAttempts++;
                }
                session.transport.close(session.closeStatus);
            } catch (Exception exception) {
                log.warn("Realtime close failed: {}", exception.getClass().getSimpleName());
            } finally {
                synchronized (session) {
                    session.closeRunning = false;
                }
            }
        })) {
            synchronized (session) {
                session.closeRunning = false;
            }
        }
    }

    /**
     * 底层连接确认结束后才释放槽位，逻辑关闭期间仍占配额
     */
    public void terminated(WebSocketSession transport) {
        RealtimeSession session = transports.get(transport.getId());
        if (session == null) {
            return;
        }
        requestClose(session, 1000, "transport terminated");
        synchronized (session) {
            if (session.terminated) {
                return;
            }
            session.terminated = true;
        }
        transports.remove(transport.getId(), session);
        session.reservation.close();
    }

    /**
     * 检查凭证期限与最近一次身份复核是否仍有效
     */
    private boolean isAuthorizationExpired(RealtimeSession session) {
        synchronized (session) {
            return !Instant.now().isBefore(session.authExpiresAt)
                    || System.nanoTime() - session.lastVerifiedAt >= VERIFY_DEADLINE;
        }
    }

    /**
     * 仅作内存判断和有界投递，调度线程不访问 Redis、数据库或网络
     */
    public void maintain() {
        long now = System.nanoTime();
        for (RealtimeSession session : transports.values()) {
            boolean closed;
            synchronized (session) {
                closed = session.closed;
            }
            if (closed) {
                scheduleClose(session);
                continue;
            }
            if (isAuthorizationExpired(session)) {
                requestClose(session, 4012, "authorization expired");
                continue;
            }
            boolean heartbeatExpired;
            boolean verify;
            String pingId = null;
            synchronized (session) {
                heartbeatExpired = now - session.lastPongAt >= PONG_TIMEOUT;
                verify = !session.verifying && now >= session.nextVerifyAt;
                if (verify) {
                    session.verifying = true;
                }
                if (now >= session.nextPingAt && session.pendingPingId == null) {
                    pingId = UUID.randomUUID().toString();
                    session.pendingPingId = pingId;
                }
            }
            if (heartbeatExpired) {
                requestClose(session, 4011, "heartbeat timeout");
                continue;
            }
            if (verify && !submitMaintenance(() -> verify(session))) {
                synchronized (session) {
                    session.verifying = false;
                }
            }
            if (pingId != null) {
                offer(session, new RealtimeServerMessage(1, pingId, "heartbeat.ping", Map.of()));
            }
        }
        if (now - lastPresenceAt >= TimeUnit.SECONDS.toNanos(5)) {
            lastPresenceAt = now;
            PresenceSnapshot snapshot = presence();
            if (snapshot.onlineUsers() != lastOnlineUsers || snapshot.onlineConnections() != lastOnlineConnections) {
                lastOnlineUsers = snapshot.onlineUsers();
                lastOnlineConnections = snapshot.onlineConnections();
                broadcast("presence", RealtimeServerMessage.create("presence.snapshot", snapshot));
            }
        }
    }

    /**
     * 复核连接身份及主题权限，并更新校验时间
     */
    private void verify(RealtimeSession session) {
        try {
            var identity = verifier.verify(session.accessToken());
            synchronized (session) {
                if (session.closed) {
                    return;
                }
                if (System.nanoTime() - session.lastVerifiedAt >= VERIFY_DEADLINE) {
                    throw new BadCredentialsException("Authorization check expired");
                }
                if (!identity.userId().equals(session.userId)
                        || (session.topics.contains("presence") && !identity.presenceAllowed())) {
                    throw new AccessDeniedException("Authorization changed");
                }
                session.lastVerifiedAt = System.nanoTime();
                session.nextVerifyAt = session.lastVerifiedAt + TimeUnit.SECONDS.toNanos(15);
            }
        } catch (Exception exception) {
            requestClose(session, 4001, "credential revoked");
        } finally {
            synchronized (session) {
                session.verifying = false;
            }
        }
    }

    /**
     * 拒绝时返回 false，由调用方清理；不借用调用线程执行任务
     */
    public boolean submitMaintenance(Runnable task) {
        try {
            maintenance.execute(task);
            return true;
        } catch (RejectedExecutionException exception) {
            long rejected = rejectedTasks.incrementAndGet();
            long now = System.nanoTime();
            long previous = lastRejectionWarning.get();
            if (now - previous > TimeUnit.SECONDS.toNanos(10)
                    && lastRejectionWarning.compareAndSet(previous, now)) {
                log.warn("Realtime maintenance queue full: rejected={}, transports={}", rejected, transports.size());
            }
            return false;
        }
    }

    public boolean isStopping() {
        return stopping.get();
    }

    /**
     * 停止接收新连接，并关闭现有连接
     */
    @PreDestroy
    public void shutdown() {
        stopping.set(true);
        for (RealtimeSession session : transports.values()) requestClose(session, 1001, "server shutdown");
    }
}
