package com.youlai.boot.system.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.system.converter.DictConverter;
import com.youlai.boot.system.mapper.DictMapper;
import com.youlai.boot.system.model.entity.Dict;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.model.form.DictForm;
import com.youlai.boot.system.model.query.DictPageQuery;
import com.youlai.boot.system.model.vo.DictPageVO;
import com.youlai.boot.system.service.DictItemService;
import com.youlai.boot.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典业务实现类
 *
 * @author haoxr
 * @since 2022/10/12
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    private final DictItemService dictItemService;
    private final DictConverter dictConverter;

    /**
     * 字典分页列表
     *
     * @param queryParams 分页查询对象
     */
    @Override
    public Page<DictPageVO> getDictPage(DictPageQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();

        // 查询数据
        return this.baseMapper.getDictPage(new Page<>(pageNum, pageSize), queryParams);
    }

    /**
     * 获取字典列表
     *
     * @return 字典列表
     */
    @Override
    public List<Option<String>> getDictList() {
        return this.list(new LambdaQueryWrapper<Dict>().eq(Dict::getStatus, 1))
                .stream()
                .map(item -> new Option<>(item.getDictCode(), item.getName()))
                .toList();
    }


    /**
     * 新增字典
     *
     * @param dictForm 字典表单数据
     */
    @Override
    public boolean saveDict(DictForm dictForm) {
        // 保存字典
        Dict entity = dictConverter.toEntity(dictForm);

        // 校验 code 是否唯一
        String dictCode = entity.getDictCode();

        long count = this.count(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictCode, dictCode)
        );

        Assert.isTrue(count == 0, "字典编码已存在");

        return this.save(entity);
    }


    /**
     * 获取字典表单详情
     *
     * @param id 字典ID
     */
    @Override
    public DictForm getDictForm(Long id) {
        // 获取字典
        Dict entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("字典不存在");
        }
        return dictConverter.toForm(entity);
    }

    /**
     * 修改字典
     *
     * @param id       字典ID
     * @param dictForm 字典表单
     */
    @Override
    @Transactional
    public boolean updateDict(Long id, DictForm dictForm) {
        // 获取字典
        Dict entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("字典不存在");
        }
        // 校验 code 是否唯一
        String dictCode = dictForm.getDictCode();
        if (!entity.getDictCode().equals(dictCode)) {
            long count = this.count(new LambdaQueryWrapper<Dict>()
                    .eq(Dict::getDictCode, dictCode)
            );
            Assert.isTrue(count == 0, "字典编码已存在");
        }
        // 更新字典
        Dict dict = dictConverter.toEntity(dictForm);
        dict.setId(id);
        boolean result = this.updateById(dict);
        if (result) {
            // 更新字典数据
            List<DictItem> dictItemList = dictItemService.list(
                    new LambdaQueryWrapper<DictItem>()
                            .eq(DictItem::getDictCode, entity.getDictCode())
                            .select(DictItem::getId)
            );
            if (!dictItemList.isEmpty()){
                List<Long> dictItemIds = dictItemList.stream().map(DictItem::getId).toList();
                DictItem dictItem = new DictItem();
                dictItem.setDictCode(dict.getDictCode());
                dictItemService.update(dictItem,
                        new LambdaQueryWrapper<DictItem>()
                                .in(DictItem::getId, dictItemIds)
                );
            }
        }
        return result;
    }

    /**
     * 删除字典
     *
     * @param ids 字典ID，多个以英文逗号(,)分割
     */
    @Transactional
    @Override
    public void deleteDictByIds(List<String> ids) {
        // 删除字典
        this.removeByIds(ids);

        // 删除字典项
        List<Dict> list = this.listByIds(ids);
        if (!list.isEmpty()) {
            List<String> dictCodes = list.stream().map(Dict::getDictCode).toList();
            dictItemService.remove(new LambdaQueryWrapper<DictItem>()
                    .in(DictItem::getDictCode, dictCodes)
            );
        }
    }

    /**
     * 根据字典ID列表获取字典编码列表
     *
     * @param ids 字典ID列表
     * @return 字典编码列表
     */
    @Override
    public List<String> getDictCodesByIds(List<String> ids) {
        List<Dict> dictList = this.listByIds(ids);
        return dictList.stream().map(Dict::getDictCode).toList();
    }

}




