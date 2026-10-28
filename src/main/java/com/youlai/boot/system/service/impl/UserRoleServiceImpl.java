package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.system.mapper.UserRoleMapper;
import com.youlai.boot.system.model.entity.UserRole;
import com.youlai.boot.system.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 用户与角色的关联读写
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    /**
     * 替换角色集合；空列表表示移除全部角色，会话撤销由用户用例负责
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceUserRoles(Long userId, List<Long> roleIds) {
        Objects.requireNonNull(userId, "userId");
        Set<Long> existing = new HashSet<>(list(new LambdaQueryWrapper<UserRole>()
                .select(UserRole::getRoleId).eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList());
        Set<Long> requested = roleIds == null ? Set.of() : new HashSet<>(roleIds);
        if (existing.equals(requested)) return false;

        Set<Long> added = new HashSet<>(requested);
        added.removeAll(existing);
        Set<Long> removed = new HashSet<>(existing);
        removed.removeAll(requested);

        if (!added.isEmpty()) {
            saveBatch(added.stream().map(roleId -> new UserRole(userId, roleId)).toList());
        }
        if (!removed.isEmpty()) {
            remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
                    .in(UserRole::getRoleId, removed));
        }
        return true;
    }

    @Override
    public boolean hasAssignedUsers(Long roleId) {
        return baseMapper.countUsersByRoleId(roleId) > 0;
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return roleId == null ? List.of() : baseMapper.listUserIdsByRoleId(roleId);
    }
}
