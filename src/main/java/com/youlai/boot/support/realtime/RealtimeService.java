package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.config.RealtimeProperties;
import com.youlai.boot.support.realtime.protocol.RealtimeServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

/**
 * 异步广播与分批发送业务变更提示
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeService {
    private static final int RECIPIENT_BATCH_SIZE = 100;
    private final RealtimeSessionManager sessions;
    private final RealtimeProperties properties;
    private final Semaphore deliveryJobs = new Semaphore(2);

    /**
     * 向订阅指定主题的连接异步广播消息
     */
    public void broadcast(String topic, String type, Object payload) {
        if (properties.isEnabled()) {
            var message = RealtimeServerMessage.create(type, payload);
            sessions.submitMaintenance(() -> sessions.broadcast(topic, message));
        }
    }

    /**
     * 分批投递失效提示，批次之间重新排队让出线程
     * loadRecipients 按 userId 升序查询游标之后的用户，返回数不得超过传入的 limit
     */
    public void sendBatches(String type, Object payload, BiFunction<Long, Integer, List<Long>> loadRecipients) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!deliveryJobs.tryAcquire()) {
            log.warn("Realtime delivery dropped: audience queue full");
            return;
        }
        submitBatch(loadRecipients, RealtimeServerMessage.create(type, payload), 0);
    }

    /**
     * 提交一批收件人投递，结束或拒绝时释放任务配额
     */
    private void submitBatch(BiFunction<Long, Integer, List<Long>> loader,
                             RealtimeServerMessage message, long afterUserId) {
        if (!sessions.submitMaintenance(() -> {
            boolean continued = false;
            try {
                List<Long> recipients = loader.apply(afterUserId, RECIPIENT_BATCH_SIZE);
                if (!recipients.isEmpty()) {
                    sessions.sendToUsers(recipients, message);
                }
                if (recipients.size() == RECIPIENT_BATCH_SIZE) {
                    continued = true;
                    submitBatch(loader, message, recipients.get(recipients.size() - 1));
                }
            } catch (Exception exception) {
                log.warn("Realtime audience delivery failed: {}", exception.getClass().getSimpleName());
            } finally {
                if (!continued) {
                    deliveryJobs.release();
                }
            }
        })) {
            deliveryJobs.release();
        }
    }
}
