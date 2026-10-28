package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.config.RealtimeProperties;
import com.youlai.boot.support.realtime.protocol.RealtimeServerMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务消息封装、游标分批与投递配额释放
 *
 * @since 4.13.0
 */
class RealtimeServiceTest {
    private final RealtimeSessionManager sessions = mock(RealtimeSessionManager.class);
    private final RealtimeProperties properties = new RealtimeProperties();
    private final RealtimeService service = new RealtimeService(sessions, properties);
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

    @BeforeEach
    void captureMaintenanceTasks() {
        when(sessions.submitMaintenance(any())).thenAnswer(call -> tasks.offer(call.getArgument(0)));
    }

    @Test
    void createsServerEnvelopeInsideService() {
        Map<String, String> payload = Map.of("dictCode", "gender");
        service.broadcast("dict", "dict.changed", payload);
        tasks.remove().run();

        var message = ArgumentCaptor.forClass(RealtimeServerMessage.class);
        verify(sessions).broadcast(eq("dict"), message.capture());
        assertThat(message.getValue().version()).isEqualTo(1);
        assertThat(message.getValue().id()).isNotBlank();
        assertThat(message.getValue().payload()).isSameAs(payload);
    }

    @Test
    void passesBatchLimitAndAdvancesCursorBetweenTasks() {
        List<Long> cursors = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();
        service.sendBatches("notice.changed", Map.of("noticeId", "1"), (after, limit) -> {
            cursors.add(after);
            limits.add(limit);
            return after == 0 ? LongStream.rangeClosed(1, limit).boxed().toList() : List.of(101L);
        });

        tasks.remove().run();
        assertThat(cursors).containsExactly(0L);
        assertThat(tasks).hasSize(1);
        tasks.remove().run();

        assertThat(cursors).containsExactly(0L, 100L);
        assertThat(limits).containsExactly(100, 100);
        assertThat(tasks).isEmpty();
        verify(sessions, times(2)).sendToUsers(anyCollection(), any());
    }

    @Test
    void releasesJobSlotAfterQueueRejectionOrLoaderFailure() {
        doReturn(false).when(sessions).submitMaintenance(any());
        service.sendBatches("notice.changed", Map.of(), (after, limit) -> List.of());
        service.sendBatches("notice.changed", Map.of(), (after, limit) -> List.of());
        when(sessions.submitMaintenance(any())).thenAnswer(call -> tasks.offer(call.getArgument(0)));
        service.sendBatches("notice.changed", Map.of(), (after, limit) -> {
            throw new IllegalStateException("query failed");
        });
        tasks.remove().run();

        service.sendBatches("notice.changed", Map.of(), (after, limit) -> List.of());
        service.sendBatches("notice.changed", Map.of(), (after, limit) -> List.of());
        assertThat(tasks).hasSize(2);
    }

    @Test
    void disabledFeatureDoesNotLoadRecipientsOrSubmitWork() {
        properties.setEnabled(false);
        BiFunction<Long, Integer, List<Long>> loader = (after, limit) -> {
            throw new AssertionError("实时通信关闭时不应查询接收人");
        };
        service.broadcast("dict", "dict.changed", Map.of());
        service.sendBatches("notice.changed", Map.of(), loader);

        verify(sessions, never()).submitMaintenance(any());
    }
}
