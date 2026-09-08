package com.youlai.boot.module.workflow.util;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.module.workflow.model.vo.ApprovalHistoryVO;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.task.Comment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批记录辅助类（用户任务含进行中节点 + 引擎评论按 taskId 归位）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Component
@RequiredArgsConstructor
public class ApprovalHistoryHelper {

    private final HistoryService historyService;
    private final TaskService taskService;

    /**
     * 获取流程实例的审批记录（按任务开始时间正序，含当前办理中节点）
     *
     * @param processInstanceId 流程实例ID
     * @return 审批记录集合
     */
    public List<ApprovalHistoryVO> getHistory(String processInstanceId) {
        // 不限 finished：进行中节点 endTime 为空，时间线据此展示"进行中"态
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        if (activities.isEmpty()) {
            return List.of();
        }
        // 同一节点驳回后重办会产生多条记录，评论按 taskId 精确归位、后写覆盖先写
        Map<String, String> comments = taskService.getProcessInstanceComments(processInstanceId).stream()
                .filter(comment -> StrUtil.isNotBlank(comment.getTaskId()))
                .collect(Collectors.toMap(Comment::getTaskId, Comment::getFullMessage, (first, second) -> second));

        return activities.stream().map(activity -> {
            ApprovalHistoryVO historyVO = new ApprovalHistoryVO();
            historyVO.setActivityId(activity.getActivityId());
            historyVO.setActivityName(activity.getActivityName());
            historyVO.setAssignee(activity.getAssignee());
            historyVO.setComment(comments.get(activity.getTaskId()));
            historyVO.setStartTime(activity.getStartTime());
            historyVO.setEndTime(activity.getEndTime());
            return historyVO;
        }).toList();
    }

}
