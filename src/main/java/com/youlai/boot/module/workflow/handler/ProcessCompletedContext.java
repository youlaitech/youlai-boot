package com.youlai.boot.module.workflow.handler;

/**
 * 流程完成上下文（路由业务后处理时携带的实例信息）
 *
 * @param processInstanceId    流程实例ID
 * @param processDefinitionKey 流程定义Key（BPMN 中 process 节点的 id）
 * @param businessKey          业务Key（本系统约定为发起时提交的表单数据ID）
 * @param startUserId          发起人账号（发起时写入 ACT_HI_PROCINST.START_USER_ID_）
 * @author Ray.Hao
 * @since 4.9.0
 */
public record ProcessCompletedContext(
        String processInstanceId,
        String processDefinitionKey,
        String businessKey,
        String startUserId) {
}
