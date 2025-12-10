package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.UserTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户租户关联 Mapper
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface UserTenantMapper extends BaseMapper<UserTenant> {
}

