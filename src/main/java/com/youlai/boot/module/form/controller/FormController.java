package com.youlai.boot.module.form.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.framework.web.util.IPUtils;
import com.youlai.boot.module.form.model.form.FormAiGenerateForm;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.module.form.model.form.FormMenuForm;
import com.youlai.boot.module.form.model.query.FormDataPageQuery;
import com.youlai.boot.module.form.model.query.FormDefinitionPageQuery;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import com.youlai.boot.module.form.model.vo.FormMenuVO;
import com.youlai.boot.module.form.model.vo.FormRenderVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.form.service.FormDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 表单控制层（表单定义、访问菜单、表单数据）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Tag(name = "12.表单接口")
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormDefinitionService formDefinitionService;
    private final FormDataService formDataService;

    @Operation(summary = "表单定义分页列表")
    @GetMapping
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.LIST)
    public PageResult<FormDefinitionPageVO> getFormDefinitionPage(
            @Valid FormDefinitionPageQuery queryParams
    ) {
        Page<FormDefinitionPageVO> result = formDefinitionService.getFormDefinitionPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "获取审批表单下拉选项（工作流设计器绑定表单用）")
    @GetMapping("/options")
    public Result<List<Option<String>>> listWorkflowFormOptions() {
        return Result.success(formDefinitionService.listWorkflowFormOptions());
    }

    @Operation(summary = "获取表单定义表单数据")
    @GetMapping("/{formId}/form")
    public Result<FormDefinitionForm> getFormDefinitionForm(
            @Parameter(description = "表单ID") @PathVariable Long formId
    ) {
        FormDefinitionForm formData = formDefinitionService.getFormDefinitionForm(formId);
        return Result.success(formData);
    }

    @Operation(summary = "新增表单定义")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('form:definition:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.INSERT)
    public Result<Void> saveFormDefinition(@Valid @RequestBody FormDefinitionForm formData) {
        boolean result = formDefinitionService.saveFormDefinition(formData);
        return Result.judge(result);
    }

    @Operation(summary = "修改表单定义")
    @PutMapping("/{formId}")
    @PreAuthorize("@ss.hasPerm('form:definition:update')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateFormDefinition(
            @Parameter(description = "表单ID") @PathVariable Long formId,
            @RequestBody @Valid FormDefinitionForm formData
    ) {
        boolean result = formDefinitionService.updateFormDefinition(formId, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除表单定义")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('form:definition:delete')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.DELETE)
    public Result<Void> removeFormDefinition(
            @Parameter(description = "表单ID，多个以英文逗号(,)拼接") @PathVariable String ids
    ) {
        formDefinitionService.removeFormDefinition(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }

    @Operation(summary = "发布表单")
    @PutMapping("/{formId}/publish")
    @PreAuthorize("@ss.hasPerm('form:definition:update')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.UPDATE)
    public Result<Void> publishFormDefinition(
            @Parameter(description = "表单ID") @PathVariable Long formId
    ) {
        boolean result = formDefinitionService.publishFormDefinition(formId);
        return Result.judge(result);
    }

    @Operation(summary = "停用表单")
    @PutMapping("/{formId}/disable")
    @PreAuthorize("@ss.hasPerm('form:definition:update')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.UPDATE)
    public Result<Void> disableFormDefinition(
            @Parameter(description = "表单ID") @PathVariable Long formId
    ) {
        boolean result = formDefinitionService.disableFormDefinition(formId);
        return Result.judge(result);
    }

    @Operation(summary = "获取表单渲染规则")
    @GetMapping("/{formKey}/render")
    public Result<FormRenderVO> getFormRender(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey
    ) {
        FormRenderVO renderVO = formDefinitionService.getFormRender(formKey);
        return Result.success(renderVO);
    }

    //---------------------------------------------------
    // 表单菜单相关接口（发布向导）
    //---------------------------------------------------
    @Operation(summary = "获取表单菜单配置（向导回显）")
    @GetMapping("/{formId}/menu")
    public Result<FormMenuVO> getFormMenu(
            @Parameter(description = "表单ID") @PathVariable Long formId
    ) {
        FormMenuVO menuVO = formDefinitionService.getFormMenu(formId);
        return Result.success(menuVO);
    }

    @Operation(summary = "生成表单访问菜单")
    @PostMapping("/{formId}/menu")
    @PreAuthorize("@ss.hasPerm('form:definition:update')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.INSERT)
    public Result<Void> saveFormMenu(
            @Parameter(description = "表单ID") @PathVariable Long formId,
            @Valid @RequestBody FormMenuForm menuForm
    ) {
        boolean result = formDefinitionService.saveFormMenu(formId, menuForm);
        return Result.judge(result);
    }

    //---------------------------------------------------
    // 公开表单相关接口（匿名，Security 白名单 + Service 校验 is_public + IP 限流）
    //---------------------------------------------------
    @Operation(summary = "获取公开表单渲染规则（匿名）")
    @GetMapping("/public/{formKey}/render")
    public Result<FormRenderVO> getPublicFormRender(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey
    ) {
        FormRenderVO renderVO = formDefinitionService.getPublicFormRender(formKey);
        return Result.success(renderVO);
    }

    @Operation(summary = "匿名提交公开表单数据")
    @PostMapping("/public/{formKey}/data")
    @RepeatSubmit
    public Result<Void> savePublicFormData(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request
    ) {
        boolean result = formDataService.savePublicFormData(formKey, data, IPUtils.getIpAddr(request));
        return Result.judge(result);
    }

    //---------------------------------------------------
    // 表单数据相关接口
    //---------------------------------------------------
    @Operation(summary = "表单数据分页列表")
    @GetMapping("/{formKey}/data")
    @PreAuthorize("@ss.hasPerm('form:data:list')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.LIST)
    public PageResult<FormDataVO> getFormDataPage(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey,
            @Valid FormDataPageQuery queryParams
    ) {
        Page<FormDataVO> result = formDataService.getFormDataPage(formKey, queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "提交表单数据")
    @PostMapping("/{formKey}/data")
    @RepeatSubmit
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.INSERT)
    public Result<Void> saveFormData(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey,
            @RequestBody Map<String, Object> data
    ) {
        boolean result = formDataService.saveFormData(formKey, data);
        return Result.judge(result);
    }

    @Operation(summary = "获取表单数据详情（只读回显）")
    @GetMapping("/{formKey}/data/{dataId}")
    @PreAuthorize("@ss.hasPerm('form:data:list')")
    public Result<FormDataVO> getFormDataDetail(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey,
            @Parameter(description = "数据ID") @PathVariable Long dataId
    ) {
        FormDataVO dataVO = formDataService.getFormDataDetail(dataId);
        // 直查数据详情时明确提示；工作流回显场景由服务层调用方自行降级
        if (dataVO == null) {
            throw new BusinessException("表单数据不存在");
        }
        return Result.success(dataVO);
    }

    @Operation(summary = "删除表单数据")
    @DeleteMapping("/{formKey}/data/{ids}")
    @PreAuthorize("@ss.hasPerm('form:data:delete')")
    @Log(module = LogModuleEnum.FORM, value = ActionTypeEnum.DELETE)
    public Result<Void> removeFormData(
            @Parameter(description = "表单唯一标识") @PathVariable String formKey,
            @Parameter(description = "数据ID，多个以英文逗号(,)拼接") @PathVariable String ids
    ) {
        formDataService.removeFormData(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }

    //---------------------------------------------------
    // AI 辅助接口（未开启 AI 时调用返回业务异常）
    //---------------------------------------------------

    @Operation(summary = "AI 生成表单规则")
    @PostMapping("/ai-generate")
    public Result<String> generateRule(@RequestBody @Valid FormAiGenerateForm form) {
        return Result.success(formDefinitionService.aiGenerateRule(form.getDescription()));
    }

}
