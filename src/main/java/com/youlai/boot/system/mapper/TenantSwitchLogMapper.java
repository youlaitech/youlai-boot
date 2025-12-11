package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.TenantSwitchLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户切换审计日志 Mapper
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface TenantSwitchLogMapper extends BaseMapper<TenantSwitchLog> {
}
