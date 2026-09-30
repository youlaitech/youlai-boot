package com.youlai.boot.module.workflow.service;

/**
 * 演示流程服务接口（请假、报销、采购、用章）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface WorkflowDemoService {

    /**
     * 幂等初始化演示流程（已存在则跳过）：部署流程定义 + 创建模型草稿
     */
    void initIfAbsent();

    /**
     * 重置工作流数据（恢复出厂）：清空所有流程定义、模型、实例与历史（含用户自建）及关联表单提交数据后重建演示流程
     */
    void reset();

}
