package com.youlai.boot.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.support.sse.SseService;
import com.youlai.boot.support.sse.constant.SseTopics;
import com.youlai.boot.system.model.dto.DictChangeDTO;
import com.youlai.boot.system.model.form.DictForm;
import com.youlai.boot.system.model.form.DictItemForm;
import com.youlai.boot.system.model.query.DictQuery;
import com.youlai.boot.system.model.query.DictItemQuery;
import com.youlai.boot.system.model.vo.DictItemOptionVO;
import com.youlai.boot.system.model.vo.DictItemPageVO;
import com.youlai.boot.system.model.vo.DictPageVO;
import com.youlai.boot.system.service.DictItemService;
import com.youlai.boot.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 字典控制层
 *
 * @author Ray Hao
 * @since 2022/10/12
 */
@Tag(name = "06.字典接口")
@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;
    private final DictItemService dictItemService;
    private final SseService sseService;

    /**
     * 广播字典变更事件到所有 SSE 在线连接
     *
     * @param dictCode 字典编码
     */
    private void sendDictChange(String dictCode) {
        sseService.broadcast(SseTopics.DICT, new DictChangeDTO(dictCode));
    }

    @Operation(summary = "字典分页列表")
    @GetMapping
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.LIST)
    public PageResult<DictPageVO> getDictPage(
            @Valid DictQuery queryParams
    ) {
        Page<DictPageVO> result = dictService.getDictPage(queryParams);
        return PageResult.success(result);
    }


    @Operation(summary = "字典列表")
    @GetMapping("/options")
    public Result<List<Option<String>>> getDictList() {
        List<Option<String>> list = dictService.getDictList();
        return Result.success(list);
    }

    @Operation(summary = "获取字典表单数据")
    @GetMapping("/{dictId}/form")
    public Result<DictForm> getDictForm(
            @Parameter(description = "字典ID") @PathVariable Long dictId
    ) {
        DictForm formData = dictService.getDictForm(dictId);
        return Result.success(formData);
    }

    @Operation(summary = "新增字典")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.INSERT)
    public Result<Void> saveDict(@Valid @RequestBody DictForm formData) {
        boolean result = dictService.saveDict(formData);
        // 发送字典更新通知
        if (result) {
            sendDictChange(formData.getDictCode());
        }
        return Result.judge(result);
    }

    @Operation(summary = "修改字典")
    @PutMapping("/{dictId}")
    @PreAuthorize("@ss.hasPerm('sys:dict:update')")
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateDict(
            @PathVariable Long dictId,
            @RequestBody @Valid DictForm formData
    ) {
        boolean status = dictService.updateDict(dictId, formData);
        // 发送字典更新通知
        if (status && formData.getDictCode() != null) {
            sendDictChange(formData.getDictCode());
        }
        return Result.judge(status);
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:dict:delete')")
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteDictionaries(
            @Parameter(description = "字典ID，多个以英文逗号(,)拼接") @PathVariable String ids
    ) {
        // 获取字典编码列表，用于发送删除通知
        List<String> dictCodes = dictService.getDictCodesByIds(Arrays.stream(ids.split(",")).toList());

        dictService.deleteDictByIds(Arrays.stream(ids.split(",")).toList());

        // 发送字典删除通知
        for (String dictCode : dictCodes) {
            sendDictChange(dictCode);
        }

        return Result.success();
    }

    @Operation(summary = "获取字典项分页列表")
    @GetMapping("/{dictCode}/items")
    public PageResult<DictItemPageVO> getDictItemsPage(
            @Parameter(description = "字典编码") @PathVariable String dictCode,
            @Valid DictItemQuery queryParams
    ) {
        queryParams.setDictCode(dictCode);
        Page<DictItemPageVO> result = dictItemService.getDictItemPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "获取字典项列表")
    @GetMapping("/{dictCode}/items/options")
    public Result<List<DictItemOptionVO>> getDictItems(
            @Parameter(description = "字典编码") @PathVariable String dictCode
    ) {
        List<DictItemOptionVO> list = dictItemService.getDictItems(dictCode);
        return Result.success(list);
    }

    @Operation(summary = "获取字典项表单数据")
    @GetMapping("/{dictCode}/items/{itemId}/form")
    public Result<DictItemForm> getDictItemForm(
            @PathVariable String dictCode,
            @Parameter(description = "字典ID") @PathVariable Long itemId
    ) {
        DictItemForm formData = dictItemService.getDictItemForm(itemId);
        return Result.success(formData);
    }

    @Operation(summary = "新增字典项")
    @PostMapping(value = "/{dictCode}/items")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.INSERT)
    public Result<Void> saveDictItem(
            @PathVariable String dictCode,
            @Valid @RequestBody DictItemForm formData
    ) {
        formData.setDictCode(dictCode);
        boolean result = dictItemService.saveDictItem(formData);

        // 发送字典更新通知
        if (result) {
            sendDictChange(dictCode);
        }

        return Result.judge(result);
    }

    @Operation(summary = "修改字典项")
    @PutMapping("/{dictCode}/items/{itemId}")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:update')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateDictItem(
            @PathVariable String dictCode,
            @PathVariable Long itemId,
            @RequestBody @Valid DictItemForm formData
    ) {
        formData.setId(itemId);
        formData.setDictCode(dictCode);
        boolean status = dictItemService.updateDictItem(formData);

        // 发送字典更新通知
        if (status) {
            sendDictChange(dictCode);
        }

        return Result.judge(status);
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/{dictCode}/items/{itemIds}")
    @PreAuthorize("@ss.hasPerm('sys:dict-item:delete')")
    @Log(module = LogModuleEnum.DICT, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteDictItemByIds(
            @PathVariable String dictCode,
            @Parameter(description = "字典项ID，多个以英文逗号(,)拼接") @PathVariable String itemIds
    ) {
        dictItemService.deleteDictItemByIds(itemIds);
        // 发送字典更新通知
        sendDictChange(dictCode);
        return Result.success();
    }

}
