package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.converter.DictItemConverter;
import com.youlai.boot.system.mapper.DictItemMapper;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.model.form.DictItemForm;
import com.youlai.boot.system.model.query.DictItemPageQuery;
import com.youlai.boot.system.model.vo.DictItemOptionVO;
import com.youlai.boot.system.model.vo.DictItemPageVO;
import com.youlai.boot.system.service.DictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    /**
     * 获取字典项分页列表
     *
     * @param queryParams 查询参数
     * @return 字典项分页列表
     */
    @Override
    public Page<DictItemPageVO> getDictItemPage(DictItemPageQuery queryParams) {
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
    public DictItemForm getDictItemForm( Long itemId) {
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
    public boolean saveDictItem(DictItemForm formData) {
        DictItem entity = dictItemConverter.toEntity(formData);
        return this.save(entity);
    }

    /**
     * 更新字典项
     *
     * @param formData 字典项表单
     * @return 是否成功
     */
    @Override
    public boolean updateDictItem(DictItemForm formData) {
        DictItem entity = dictItemConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除字典项
     *
     * @param ids 字典项ID集合
     */
    @Override
    public void deleteDictItemByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
          .map(Long::parseLong)
          .toList();
        this.removeByIds(idList);
    }

}




