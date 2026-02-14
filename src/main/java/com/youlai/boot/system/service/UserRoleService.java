package com.youlai.boot.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.system.model.entity.UserRole;

import java.util.List;

/**
 * 用户角色业务接口
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 保存用户角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return
     */
    void saveUserRoles(Long userId, List<Long> roleIds);

    /**
     * 判断角色是否存在绑定的用户
     *
     * @param roleId 角色ID
     * @return true：已分配 false：未分配
     */
    boolean hasAssignedUsers(Long roleId);

    /**
     * 获取角色绑定的用户ID集合
     *
     * @param roleId 角色ID
     * @return 用户ID集合
     */
    List<Long> listUserIdsByRoleId(Long roleId);
}
