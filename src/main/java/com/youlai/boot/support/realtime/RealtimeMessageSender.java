package com.youlai.boot.support.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 每连接一个发送者；排队帧和正在写出的帧都计入内存预算
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Slf4j
@Component
public class RealtimeMessageSender {
    private static final int MAX_SESSION_BYTES = 32_768;
    private static final int MAX_SESSION_MESSAGES = 32;
    private final Executor executor;

    public RealtimeMessageSender(@Qualifier("realtimeSenderExecutor") Executor executor) {
        this.executor = executor;
    }

    /**
     * 待发送文本及其预算占用
     */
    record Frame(String text, int bytes) {
        /**
         * 按 UTF-8 字节数计算帧的预算占用
         */
        static Frame of(String text) {
            return new Frame(text, text.getBytes(StandardCharsets.UTF_8).length);
        }
    }

    /**
     * 在连接预算内入队，同一连接只启动一个发送者
     */
    boolean enqueue(RealtimeSession session, Frame frame, Runnable onFailure) {
        boolean start;
        synchronized (session) {
            if (session.closed) {
                return false;
            }
            if (session.bufferedMessages >= MAX_SESSION_MESSAGES
                    || session.bufferedBytes + frame.bytes() > MAX_SESSION_BYTES) {
                return false;
            }
            session.queue.addLast(frame);
            session.bufferedBytes += frame.bytes();
            session.bufferedMessages++;
            start = !session.draining;
            session.draining = true;
        }
        if (start) {
            submit(session, onFailure);
        }
        return true;
    }

    /**
     * 投递发送任务，线程池拒绝时清理连接
     */
    private void submit(RealtimeSession session, Runnable onFailure) {
        try {
            executor.execute(() -> drain(session, onFailure));
        } catch (RejectedExecutionException exception) {
            synchronized (session) {
                session.draining = false;
            }
            onFailure.run();
        }
    }

    /**
     * 串行发送连接中的排队帧，并归还预算
     */
    private void drain(RealtimeSession session, Runnable onFailure) {
        // 每轮最多发送 16 帧后让出线程，避免单条连接长期占用发送池
        for (int count = 0; count < 16; count++) {
            Frame frame;
            synchronized (session) {
                frame = session.closed ? null : session.queue.pollFirst();
                if (frame == null) {
                    session.draining = false;
                    return;
                }
            }
            boolean failed = false;
            try {
                session.transport.sendMessage(new TextMessage(frame.text()));
            } catch (Exception exception) {
                failed = true;
                log.debug("Realtime write failed: {}", exception.getClass().getSimpleName());
            } finally {
                // 正在写出的帧在此结算，关闭连接只释放尚未发送的队列
                synchronized (session) {
                    session.bufferedBytes -= frame.bytes();
                    session.bufferedMessages--;
                }
            }
            if (failed) {
                synchronized (session) {
                    session.draining = false;
                }
                onFailure.run();
                return;
            }
        }
        submit(session, onFailure);
    }

    /**
     * 只释放排队帧，正在写出的帧由 drain 的 finally 结算
     */
    void discardQueued(RealtimeSession session) {
        synchronized (session) {
            Frame frame;
            while ((frame = session.queue.pollFirst()) != null) {
                session.bufferedBytes -= frame.bytes();
                session.bufferedMessages--;
            }
        }
    }
}
