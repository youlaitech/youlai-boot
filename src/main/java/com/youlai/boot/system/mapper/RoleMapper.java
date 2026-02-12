package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色持久层接口
 *
 * @author Ray.Hao
 * @since 2022/1/14
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 获取最大范围的数据权限
     *
     * @param roles 角色编码集合
     * @return {@link Integer} – 数据权限范围
     */
    Integer getMaximumDataScope(Set<String> roles);

    /**
     * 获取角色的数据权限信息列表
     * <p>
     * 返回角色编码和数据权限范围的映射列表
     *
     * @param roleCodes 角色编码集合
     * @return 角色数据权限信息列表 [{code: 'ADMIN', data_scope: 1}, ...]
     */
    List<Map<String, Object>> getRoleDataScopeList(@Param("roleCodes") Set<String> roleCodes);

}
