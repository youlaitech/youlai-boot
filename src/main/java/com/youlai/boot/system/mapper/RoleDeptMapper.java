package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.RoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色部门关联持久层
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface RoleDeptMapper extends BaseMapper<RoleDept> {

    /**
     * 根据角色ID获取部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> getDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色编码集合获取所有部门ID列表（用于自定义数据权限）
     *
     * @param roleCodes 角色编码集合
     * @return 部门ID列表
     */
    List<Long> getDeptIdsByRoleCodes(@Param("roleCodes") List<String> roleCodes);

}
