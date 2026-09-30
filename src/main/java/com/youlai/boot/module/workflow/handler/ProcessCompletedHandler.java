package com.youlai.boot.module.workflow.handler;

/**
 * 流程审批通过后的业务处理器（扩展点）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface ProcessCompletedHandler {

    /**
     * 适配的流程定义 Key（BPMN 中 process 节点的 id，如 expense）
     */
    String getProcessDefinitionKey();

    /**
     * 审批通过后的业务处理
     *
     * @param context 流程完成上下文
     */
    void handle(ProcessCompletedContext context);
}
