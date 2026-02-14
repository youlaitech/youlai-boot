package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.system.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色访问层
 *
 * @author haoxr
 * @since 2022/1/15
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 获取角色绑定的用户数
     *
     * @param roleId 角色ID
     */
    int countUsersByRoleId(Long roleId);

    /**
     * 获取角色绑定的用户ID集合
     *
     * @param roleId 角色ID
     * @return 用户ID集合
     */
    java.util.List<Long> listUserIdsByRoleId(Long roleId);
}
