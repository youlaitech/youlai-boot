package com.youlai.boot.module.workflow.handler;

/**
 * 流程审批通过后的业务处理器（扩展点）
 *
 * <p>一个流程定义 Key 对应一个处理器，由 ProcessCompletedListener 统一路由；
 * 新增"审批通过后处理"（如报销入账、采购入库、离职禁用账号）只需实现本接口并注册为 Spring Bean，
 * 无需改动审批与引擎代码</p>
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
