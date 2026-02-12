package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.system.mapper.RoleMenuMapper;
import com.youlai.boot.system.model.bo.RolePermsBO;
import com.youlai.boot.system.model.entity.RoleMenu;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色菜单服务实现类
 *
 * @author Ray.Hao
 * @since 2.5.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 刷新权限缓存
     */
    @Override
    public void refreshRolePermsCache() {
        String cacheKey = RedisConstants.System.ROLE_PERMS;

        // 清理权限缓存
        redisTemplate.delete(cacheKey);

        // 预热权限缓存，避免后续请求触发频繁回源
        List<RolePermsBO> list = this.baseMapper.getRolePermsList(null);
        if (CollectionUtil.isNotEmpty(list)) {
            list.forEach(item -> {
                String roleCode = item.getRoleCode();
                Set<String> perms = item.getPerms();
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(cacheKey, roleCode, perms);
                }
            });
        }

        log.info("权限缓存刷新完成");
    }

    /**
     * 刷新单个角色权限缓存
     */
    @Override
    public void refreshRolePermsCache(String roleCode) {
        String cacheKey = RedisConstants.System.ROLE_PERMS;

        // 清理指定角色缓存
        redisTemplate.opsForHash().delete(cacheKey, roleCode);

        // 回源 DB 并更新缓存
        List<RolePermsBO> list = this.baseMapper.getRolePermsList(roleCode);
        if (CollectionUtil.isNotEmpty(list)) {
            RolePermsBO rolePerms = list.get(0);
            if (rolePerms != null) {
                Set<String> perms = rolePerms.getPerms();
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(cacheKey, roleCode, perms);
                }
            }
        }

        log.info("角色[{}]权限缓存刷新完成", roleCode);
    }

    /**
     * 刷新权限缓存（角色编码变更时调用）
     */
    @Override
    public void refreshRolePermsCache(String oldRoleCode, String newRoleCode) {
        String cacheKey = RedisConstants.System.ROLE_PERMS;

        // 清理旧角色权限缓存
        redisTemplate.opsForHash().delete(cacheKey, oldRoleCode);
        redisTemplate.opsForHash().delete(cacheKey, newRoleCode);

        // 回源 DB 并更新新角色编码缓存
        List<RolePermsBO> list = this.baseMapper.getRolePermsList(newRoleCode);
        if (CollectionUtil.isNotEmpty(list)) {
            RolePermsBO rolePerms = list.get(0);
            if (rolePerms != null) {
                Set<String> perms = rolePerms.getPerms();
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(cacheKey, newRoleCode, perms);
                }
            }
        }

        log.info("角色编码变更: {} -> {}，相关权限缓存刷新完成", oldRoleCode, newRoleCode);
    }

    /**
     * 获取角色权限集合（带缓存）
     * <p>
     * 采用 Read-Through 缓存策略：
     * <ol>
     *   <li>优先从 Redis Hash 缓存读取</li>
     *   <li>缓存未命中时回源 DB 并写入缓存</li>
     * </ol>
     *
     * @param roleCodes 角色编码集合
     * @return 权限集合
     */
    @Override
    public Set<String> getRolePermsByRoleCodes(Set<String> roleCodes) {
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptySet();
        }

        String cacheKey = RedisConstants.System.ROLE_PERMS;
        Set<String> perms = new HashSet<>();
        List<String> roleCodeList = new ArrayList<>(roleCodes);

        // 1. 尝试从缓存批量获取
        List<Object> cachedPermsList = redisTemplate.opsForHash().multiGet(cacheKey, new ArrayList<>(roleCodeList));

        List<String> missingRoles = new ArrayList<>();
        for (int i = 0; i < roleCodeList.size(); i++) {
            Object cachedPerms = cachedPermsList.get(i);
            String roleCode = roleCodeList.get(i);

            if (cachedPerms == null) {
                // 缓存未命中，记录需要回源的角色
                missingRoles.add(roleCode);
                continue;
            }

            // Redis JSON 序列化后，Set 会以 Collection 形式反序列化
            if (cachedPerms instanceof Collection<?> collection) {
                collection.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .forEach(perms::add);
            } else {
                // 兼容单个权限字符串的极端情况
                perms.add(cachedPerms.toString());
            }
        }

        // 2. 回源 DB 并同步到缓存
        if (!missingRoles.isEmpty()) {
            for (String roleCode : missingRoles) {
                Set<String> dbPerms = this.baseMapper.listRolePerms(Collections.singleton(roleCode));
                if (dbPerms == null) {
                    dbPerms = Collections.emptySet();
                }
                // 写入缓存（空集也写入，防止缓存穿透）
                redisTemplate.opsForHash().put(cacheKey, roleCode, dbPerms);
                perms.addAll(dbPerms);
            }
        }

        return perms;
    }

    /**
     * 获取角色拥有的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合
     */
    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return this.baseMapper.listMenuIdsByRoleId(roleId);
    }

}
