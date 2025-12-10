package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}

