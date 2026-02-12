package com.youlai.boot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.system.model.entity.RoleDept;

import java.util.List;

/**
 * 角色部门关联服务接口
 *
 * @author Ray.Hao
 * @since 4.1.0
 */
public interface RoleDeptService extends IService<RoleDept> {

    /**
     * 根据角色ID获取部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> getDeptIdsByRoleId(Long roleId);

    /**
     * 根据角色编码集合获取所有部门ID列表（用于自定义数据权限）
     *
     * @param roleCodes 角色编码集合
     * @return 部门ID列表
     */
    List<Long> getDeptIdsByRoleCodes(List<String> roleCodes);

    /**
     * 保存角色部门关联
     *
     * @param roleId  角色ID
     * @param deptIds 部门ID列表
     */
    void saveRoleDepts(Long roleId, List<Long> deptIds);

    /**
     * 删除角色部门关联
     *
     * @param roleId 角色ID
     */
    void deleteByRoleId(Long roleId);

}
