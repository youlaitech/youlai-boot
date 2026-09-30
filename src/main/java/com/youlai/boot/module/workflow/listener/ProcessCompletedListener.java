package com.youlai.boot.module.workflow.listener;

import com.youlai.boot.module.workflow.handler.ProcessCompletedContext;
import com.youlai.boot.module.workflow.handler.ProcessCompletedHandler;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流程完成事件监听器：审批通过（实例正常结束）后按流程定义 Key 路由到业务处理器
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Slf4j
@ConditionalOnWorkflowEnabled
@Component
public class ProcessCompletedListener implements FlowableEventListener {

    private final HistoryService historyService;
    private final Map<String, ProcessCompletedHandler> handlers;

    public ProcessCompletedListener(@Lazy HistoryService historyService, List<ProcessCompletedHandler> handlerList) {
        this.historyService = historyService;
        // 以流程定义 Key 索引处理器：新增业务后处理实现接口即自动被路由
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ProcessCompletedHandler::getProcessDefinitionKey, Function.identity()));
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() != FlowableEngineEventType.PROCESS_COMPLETED) {
            return;
        }
        String processInstanceId = ((FlowableEngineEvent) event).getProcessInstanceId();
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (instance == null) {
            return;
        }
        ProcessCompletedHandler handler = handlers.get(instance.getProcessDefinitionKey());
        if (handler == null) {
            // 未登记后处理的流程（如请假、用章）静默跳过，避免日志噪音
            log.debug("流程 [{}] 审批通过，未登记业务后处理", instance.getProcessDefinitionKey());
            return;
        }
        handler.handle(new ProcessCompletedContext(
                instance.getId(),
                instance.getProcessDefinitionKey(),
                instance.getBusinessKey(),
                instance.getStartUserId()));
    }

    /**
     * 监听器异常不回滚审批事务：后处理（入账/入库）失败不阻断流程结束
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }

    /**
     * 常规事件派发（默认）：不挂事务生命周期回调
     */
    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    /**
     * 事务生命周期回调时机（仅 isFireOnTransactionLifecycleEvent 为 true 时生效），此处无需
     */
    @Override
    public String getOnTransaction() {
        return null;
    }
}
