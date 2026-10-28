package com.youlai.boot.system.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.converter.DictItemConverter;
import com.youlai.boot.system.mapper.DictItemMapper;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.model.form.DictItemForm;
import com.youlai.boot.system.model.query.DictItemQuery;
import com.youlai.boot.system.model.vo.DictItemOptionVO;
import com.youlai.boot.system.model.vo.DictItemPageVO;
import com.youlai.boot.system.service.DictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.youlai.boot.system.event.DictChangedEvent;

import java.util.Arrays;
import java.util.List;

/**
 * 字典项实现类
 *
 * @author Ray.Hao
 * @since 2022/10/12
 */
@Service
@RequiredArgsConstructor
public class DictItemServiceImpl extends ServiceImpl<DictItemMapper, DictItem> implements DictItemService {

    private final DictItemConverter dictItemConverter;
    private final ApplicationEventPublisher events;

    /**
     * 获取字典项分页列表
     *
     * @param queryParams 查询参数
     * @return 字典项分页列表
     */
    @Override
    public Page<DictItemPageVO> getDictItemPage(DictItemQuery queryParams) {
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<DictItemPageVO> page = new Page<>(pageNum, pageSize);

        return this.baseMapper.getDictItemPage(page, queryParams);
    }

    /**
     * 获取字典项列表
     *
     * @param dictCode 字典编码
     */
    @Override
    public List<DictItemOptionVO> getDictItems(String dictCode) {
        return this.list(
                        new LambdaQueryWrapper<DictItem>()
                                .eq(DictItem::getDictCode, dictCode)
                                .eq(DictItem::getStatus, 1)
                                .orderByAsc(DictItem::getSort)
                                .orderByAsc(DictItem::getId)
                ).stream()
                .map(item -> {
                    DictItemOptionVO dictItemOptionVo = new DictItemOptionVO();
                    dictItemOptionVo.setLabel(item.getLabel());
                    dictItemOptionVo.setValue(item.getValue());
                    dictItemOptionVo.setTagType(item.getTagType());
                    return dictItemOptionVo;
                }).toList();
    }

    /**
     * 获取字典项表单
     *
     * @param itemId 字典项ID
     * @return 字典项表单
     */
    @Override
    public DictItemForm getDictItemForm(Long itemId) {
        DictItem entity = this.getById(itemId);
        return dictItemConverter.toForm(entity);
    }

    /**
     * 保存字典项
     *
     * @param formData 字典项表单
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveDictItem(DictItemForm formData) {
        validateValueUnique(formData.getDictCode(), formData.getValue(), null);
        DictItem entity = dictItemConverter.toEntity(formData);
        boolean saved = this.save(entity);
        if (saved) events.publishEvent(new DictChangedEvent(entity.getDictCode()));
        return saved;
    }

    /**
     * 更新字典项
     *
     * @param formData 字典项表单
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateDictItem(DictItemForm formData) {
        DictItem previous = this.getById(formData.getId());
        validateValueUnique(formData.getDictCode(), formData.getValue(), formData.getId());
        DictItem entity = dictItemConverter.toEntity(formData);
        boolean updated = this.updateById(entity);
        if (updated) {
            events.publishEvent(new DictChangedEvent(entity.getDictCode()));
            if (previous != null && !previous.getDictCode().equals(entity.getDictCode())) {
                events.publishEvent(new DictChangedEvent(previous.getDictCode()));
            }
        }
        return updated;
    }

    /**
     * 删除字典项
     *
     * @param ids 字典项ID集合
     */
    @Override
    @Transactional
    public void deleteDictItemByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
          .map(Long::parseLong)
          .toList();
        List<String> codes = this.listByIds(idList).stream().map(DictItem::getDictCode).distinct().toList();
        this.removeByIds(idList);
        codes.forEach(code -> events.publishEvent(new DictChangedEvent(code)));
    }

    /**
     * 校验同一字典下字典项值唯一
     *
     * @param dictCode 字典编码
     * @param value 字典项值
     * @param excludeId 更新时传当前字典项ID以排除自身，新增传 null 不排除任何记录
     */
    private void validateValueUnique(String dictCode, String value, Long excludeId) {
        long count = this.count(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictCode, dictCode)
                .eq(DictItem::getValue, value)
                .ne(excludeId != null, DictItem::getId, excludeId)
        );
        Assert.isTrue(count == 0, "该字典下字典项值已存在");
    }

}
