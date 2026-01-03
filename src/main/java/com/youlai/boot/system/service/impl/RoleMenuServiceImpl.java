package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.system.mapper.RoleMenuMapper;
import com.youlai.boot.system.model.bo.RolePermsBO;
import com.youlai.boot.system.model.entity.RoleMenu;
import com.youlai.boot.system.service.RoleMenuService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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
     * 启动时初始化权限缓存
     */
    @PostConstruct
    public void initRolePermsCache() {
        log.info("开始初始化权限缓存...");
        
        List<RolePermsBO> allRolePermsList = this.baseMapper.getRolePermsList(null);
        
        if (CollectionUtil.isEmpty(allRolePermsList)) {
            log.warn("权限数据为空，跳过缓存初始化");
            return;
        }
        
        // 所有数据统一缓存
        String cacheKey = RedisConstants.System.ROLE_PERMS;
        allRolePermsList.forEach(rolePerms -> {
            String roleCode = rolePerms.getRoleCode();
            Set<String> perms = rolePerms.getPerms();
            
            if (CollectionUtil.isNotEmpty(perms)) {
                redisTemplate.opsForHash().put(cacheKey, roleCode, perms);
            }
        });
        log.info("权限缓存初始化完成，共{}条数据", allRolePermsList.size());
    }

    /**
     * 刷新权限缓存
     */
    @Override
    public void refreshRolePermsCache() {
        String cacheKey = RedisConstants.System.ROLE_PERMS;
        
        // 清理权限缓存
        redisTemplate.delete(cacheKey);
        
        // 重新加载权限
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
        
        // 重新加载指定角色权限
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
        
        // 添加新角色权限缓存
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
        
        log.info("角色编码变更: {} -> {}，权限缓存已更新", oldRoleCode, newRoleCode);
    }

    /**
     * 获取角色权限集合
     *
     * @param roles 角色编码集合
     * @return 权限集合
     */
    @Override
    public Set<String> getRolePermsByRoleCodes(Set<String> roles) {
        // 直接查询数据库（保持原有逻辑）
        return this.baseMapper.listRolePerms(roles);
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
