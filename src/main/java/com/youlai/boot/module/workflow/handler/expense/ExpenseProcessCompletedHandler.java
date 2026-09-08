package com.youlai.boot.module.workflow.handler.expense;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.workflow.handler.ProcessCompletedContext;
import com.youlai.boot.module.workflow.handler.ProcessCompletedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报销流程审批通过后的业务处理（示例：财务入账）
 *
 * <p>演示项目的入账为日志占位，未真实对接财务系统；
 * businessKey 即发起时提交的报销表单数据ID，可回读申请人填写的金额等内容</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseProcessCompletedHandler implements ProcessCompletedHandler {

    private final FormDataService formDataService;

    /**
     * 适配的流程定义 Key：报销（对应 expense.bpmn20.xml 的 process id）
     */
    @Override
    public String getProcessDefinitionKey() {
        return "expense";
    }

    @Override
    public void handle(ProcessCompletedContext context) {
        if (StrUtil.isBlank(context.businessKey())) {
            log.info("[报销审批通过] 发起人={}，实例未关联表单数据，跳过入账", context.startUserId());
            return;
        }
        FormDataVO formData = formDataService.getFormDataDetail(Long.valueOf(context.businessKey()));
        // TODO 正式环境：调用财务系统按报销金额生成入账凭证，并将凭证号回写业务表
        log.info("[报销审批通过] 发起人={}，报销内容={} → 调用财务系统入账（演示占位，未真实执行）",
                context.startUserId(), formData == null ? null : formData.getDataJson());
    }
}
