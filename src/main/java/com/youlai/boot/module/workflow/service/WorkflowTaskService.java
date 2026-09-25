package com.youlai.boot.module.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.workflow.model.form.CompleteTaskForm;
import com.youlai.boot.module.workflow.model.form.RejectTaskForm;
import com.youlai.boot.module.workflow.model.query.WorkflowTaskPageQuery;
import com.youlai.boot.module.workflow.model.vo.DoneTaskVO;
import com.youlai.boot.module.workflow.model.vo.RejectTargetVO;
import com.youlai.boot.module.workflow.model.vo.TaskDetailVO;
import com.youlai.boot.module.workflow.model.vo.TodoTaskVO;

import java.util.List;

/**
 * 流程任务业务接口（待办/已办、审批通过/驳回）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface WorkflowTaskService {

    /**
     * 待办任务分页列表（当前用户的候选/分配任务）
     *
     * @param queryParams 查询参数
     * @return 分页结果
     */
    Page<TodoTaskVO> getTodoTaskPage(WorkflowTaskPageQuery queryParams);

    /**
     * 已办任务分页列表（当前用户已办理的任务）
     *
     * @param queryParams 查询参数
     * @return 分页结果
     */
    Page<DoneTaskVO> getDoneTaskPage(WorkflowTaskPageQuery queryParams);

    /**
     * 获取待办任务详情（发起表单只读回显 + 审批记录）
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    TaskDetailVO getTaskDetail(String taskId);

    /**
     * 获取驳回目标节点列表（当前实例已办理过的用户任务）
     *
     * @param taskId 任务ID
     * @return 驳回目标集合
     */
    List<RejectTargetVO> listRejectTargets(String taskId);

    /**
     * 审批通过（意见归档为引擎评论，变量驱动后续网关）
     *
     * @param taskId   任务ID
     * @param formData 审批表单数据
     */
    void completeTask(String taskId, CompleteTaskForm formData);

    /**
     * 驳回（流转到指定历史节点，任务与意见随历史归档）
     *
     * @param taskId   任务ID
     * @param formData 驳回表单数据
     */
    void rejectTask(String taskId, RejectTaskForm formData);

    /**
     * 用 AI 汇总表单数据与审批记录，生成审批摘要
     *
     * @param taskId 任务ID
     * @return 摘要文本
     */
    String aiSummarizeTask(String taskId);

}
