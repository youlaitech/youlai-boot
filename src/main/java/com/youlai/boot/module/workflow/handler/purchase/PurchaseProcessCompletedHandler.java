package com.youlai.boot.module.workflow.handler.purchase;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.workflow.handler.ProcessCompletedContext;
import com.youlai.boot.module.workflow.handler.ProcessCompletedHandler;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 采购流程审批通过后的业务处理（示例：采购入库）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Slf4j
@ConditionalOnWorkflowEnabled
@Component
@RequiredArgsConstructor
public class PurchaseProcessCompletedHandler implements ProcessCompletedHandler {

    private final FormDataService formDataService;

    /**
     * 适配的流程定义 Key：采购（对应 purchase.bpmn20.xml 的 process id）
     */
    @Override
    public String getProcessDefinitionKey() {
        return "purchase";
    }

    @Override
    public void handle(ProcessCompletedContext context) {
        if (StrUtil.isBlank(context.businessKey())) {
            log.info("[采购审批通过] 发起人={}，实例未关联表单数据，跳过入库", context.startUserId());
            return;
        }
        FormDataVO formData = formDataService.getFormDataDetail(Long.valueOf(context.businessKey()));
        // TODO 正式环境：调用仓储系统生成入库单并变更库存
        log.info("[采购审批通过] 发起人={}，采购内容={} → 调用仓储系统入库（演示占位，未真实执行）",
                context.startUserId(), formData == null ? null : formData.getDataJson());
    }
}
