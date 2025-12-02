package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.security.token.TokenManager;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.system.mapper.UserRoleMapper;
import com.youlai.boot.system.model.entity.UserRole;
import com.youlai.boot.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {


  private final TokenManager tokenManager;

  /**
   * 保存用户角色
   *
   * @param userId 用户ID
   * @param roleIds 选择的角色ID集合
   * @return
   */
  @Override
  public void saveUserRoles(Long userId, List<Long> roleIds) {
    if (userId == null || CollectionUtil.isEmpty(roleIds)) {
      return ;
    }

    // 获取现有角色
    List<Long> userRoleIds = this.list(new LambdaQueryWrapper<UserRole>()
        .select(UserRole::getRoleId)
        .eq(UserRole::getUserId, userId))
      .parallelStream()
      .map(UserRole::getRoleId)
      .toList();

    // 使用Set提升对比效率
    Set<Long> oldRoles = new HashSet<>(userRoleIds);
    Set<Long> newRoles = new HashSet<>(roleIds);

    // 计算变更集
    Set<Long> addedRoles = new HashSet<>(newRoles);
    addedRoles.removeAll(oldRoles);

    Set<Long> removedRoles = new HashSet<>(oldRoles);
    removedRoles.removeAll(newRoles);

    boolean rolesChanged = !addedRoles.isEmpty() || !removedRoles.isEmpty();

    // 批量保存新增角色
    if (!addedRoles.isEmpty()) {
      this.saveBatch(addedRoles.stream()
        .map(roleId -> new UserRole(userId, roleId))
        .collect(Collectors.toList()));
    }

    // 删除废弃角色
    if (!removedRoles.isEmpty()) {
      this.remove(new LambdaQueryWrapper<UserRole>()
        .eq(UserRole::getUserId, userId)
        .in(UserRole::getRoleId, removedRoles));
    }

    // 当权限变更时清除被修改用户的登录态
    if (rolesChanged) {
      tokenManager.invalidateUserSessions(userId);
    }
  }

  /**
   * 判断角色是否存在绑定的用户
   *
   * @param roleId 角色ID
   * @return true：已分配 false：未分配
   */
  @Override
  public boolean hasAssignedUsers(Long roleId) {
    int count = this.baseMapper.countUsersForRole(roleId);
    return count > 0;
  }
}
