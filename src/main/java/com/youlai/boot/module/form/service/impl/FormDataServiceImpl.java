package com.youlai.boot.module.form.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.form.enums.FormStatusEnum;
import com.youlai.boot.module.form.mapper.FormDataMapper;
import com.youlai.boot.module.form.mapper.FormSnapshotMapper;
import com.youlai.boot.module.form.model.entity.FormData;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.entity.FormSnapshot;
import com.youlai.boot.module.form.model.query.FormDataQuery;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.form.service.FormDefinitionService;
import com.youlai.boot.module.form.util.FormRuleValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 表单数据业务实现类
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormDataServiceImpl extends ServiceImpl<FormDataMapper, FormData> implements FormDataService {

    private final FormDefinitionService formDefinitionService;
    private final FormSnapshotMapper formSnapshotMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 公开表单单 IP 提交上限（次/分钟）：匿名接口防刷阈值
    private static final int PUBLIC_SUBMIT_LIMIT = 10;

    // 公开表单限流窗口：1 分钟
    private static final Duration PUBLIC_SUBMIT_WINDOW = Duration.ofMinutes(1);

    /**
     * 提交表单数据
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     */
    @Override
    public boolean saveFormData(String formKey, Map<String, Object> data) {
        return this.saveFormDataReturnId(formKey, data) != null;
    }

    /**
     * 提交表单数据并返回数据ID
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     */
    @Override
    public Long saveFormDataReturnId(String formKey, Map<String, Object> data) {
        FormDefinition form = formDefinitionService.getByFormKey(formKey);
        if (!FormStatusEnum.PUBLISHED.getValue().equals(form.getStatus())) {
            throw new BusinessException("表单未发布或已停用");
        }
        return this.persistFormData(form, data);
    }

    /**
     * 匿名提交公开表单数据
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     * @param ip      提交方 IP（限流维度）
     */
    @Override
    public boolean savePublicFormData(String formKey, Map<String, Object> data, String ip) {
        FormDefinition form = formDefinitionService.getByFormKey(formKey);
        if (!FormStatusEnum.PUBLISHED.getValue().equals(form.getStatus())
                || !Integer.valueOf(1).equals(form.getIsPublic())) {
            throw new BusinessException("表单不存在或未开放公开访问");
        }
        checkPublicSubmitLimit(formKey, ip);
        log.info("公开表单匿名提交  formKey={}  ip={}", formKey, ip);
        return this.persistFormData(form, data) != null;
    }

    /**
     * 数据落库（登录/匿名提交共用）
     *
     * @param form 表单定义
     * @param data 提交数据
     * @return 数据ID（落库失败为 null）
     */
    private Long persistFormData(FormDefinition form, Map<String, Object> data) {
        Map<String, Object> filteredData = FormRuleValidator.validateAndFilter(form.getFormJson(), data);

        FormData entity = new FormData();
        entity.setFormId(form.getId());
        entity.setFormVersion(form.getVersion());
        entity.setDataJson(JSONUtil.toJsonStr(filteredData));
        return this.save(entity) ? entity.getId() : null;
    }

    /**
     * 公开表单按 formKey + IP 限流（固定窗口计数）
     *
     * @param formKey 表单唯一标识
     * @param ip      提交方 IP
     */
    private void checkPublicSubmitLimit(String formKey, String ip) {
        String key = StrUtil.format(RedisConstants.Form.PUBLIC_SUBMIT_COUNT, formKey, ip);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null) {
                // 首次计数时设置过期，窗口结束后自动清零
                if (count == 1) {
                    redisTemplate.expire(key, PUBLIC_SUBMIT_WINDOW);
                }
                if (count > PUBLIC_SUBMIT_LIMIT) {
                    throw new BusinessException("提交过于频繁，请稍后再试");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("公开表单限流 Redis 异常，Fail-Open 放行  formKey={}  ip={}", formKey, ip, e);
        }
    }

    /**
     * 表单数据分页列表
     *
     * @param formKey     表单唯一标识
     * @param queryParams 查询参数
     */
    @Override
    public Page<FormDataVO> getFormDataPage(String formKey, FormDataQuery queryParams) {
        FormDefinition form = formDefinitionService.getByFormKey(formKey);
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        return this.baseMapper.getFormDataPage(new Page<>(pageNum, pageSize), form.getId(), queryParams);
    }

    /**
     * 获取表单数据详情（只读回显）
     *
     * @param dataId 数据ID
     */
    @Override
    public FormDataVO getFormDataDetail(Long dataId) {
        FormData entity = this.getById(dataId);
        if (entity == null) {
            return null;
        }
        FormDataVO vo = new FormDataVO();
        vo.setId(entity.getId());
        vo.setFormVersion(entity.getFormVersion());
        vo.setDataJson(entity.getDataJson());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        this.fillRuleSnapshot(vo, entity.getFormId(), entity.getFormVersion());
        return vo;
    }

    /**
     * 填充回显规则：命中版本快照用快照规则，否则回退当前定义
     *
     * @param vo      数据详情 VO
     * @param formId  表单定义ID
     * @param version 提交时版本
     */
    private void fillRuleSnapshot(FormDataVO vo, Long formId, Integer version) {
        FormSnapshot snapshot = formSnapshotMapper.selectOne(new LambdaQueryWrapper<FormSnapshot>()
                .eq(FormSnapshot::getFormId, formId)
                .eq(FormSnapshot::getVersion, version)
                .last("LIMIT 1")
        );
        if (snapshot != null) {
            vo.setFormJson(snapshot.getFormJson());
            vo.setOptionsJson(snapshot.getOptionsJson());
            return;
        }
        FormDefinition form = formDefinitionService.getFormDefinition(formId);
        if (form != null) {
            vo.setFormJson(form.getFormJson());
            vo.setOptionsJson(form.getOptionsJson());
        }
    }

    /**
     * 批量删除表单数据
     *
     * @param ids 数据ID列表
     */
    @Override
    public void removeFormData(List<Long> ids) {
        this.removeByIds(ids);
    }

    /**
     * 按表单标识删除全部提交数据（演示数据重置用）
     *
     * @param formKey 表单唯一标识
     */
    @Override
    public void removeByFormKey(String formKey) {
        FormDefinition form = formDefinitionService.getByFormKey(formKey);
        if (form == null) {
            return;
        }
        this.remove(new LambdaQueryWrapper<FormData>()
                .eq(FormData::getFormId, form.getId()));
    }

}
