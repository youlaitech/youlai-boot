package com.youlai.boot.platform.codegen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.platform.codegen.mapper.GenTableColumnMapper;
import com.youlai.boot.platform.codegen.model.entity.GenTableColumn;
import com.youlai.boot.platform.codegen.service.GenTableColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 代码生成字段配置服务实现类
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Service
@RequiredArgsConstructor
public class GenTableColumnServiceImpl extends ServiceImpl<GenTableColumnMapper, GenTableColumn> implements GenTableColumnService {


}
