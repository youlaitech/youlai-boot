package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
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
 * 角色菜单服务实现类（多租户优化版）
 *
 * @author Ray.Hao
 * @since 2.5.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TenantProperties tenantProperties;

    /**
     * 构建租户权限缓存key
     *
     * @param tenantId 租户ID
     * @return 缓存key
     *         - 多租户开启: system:role:perms:{tenantId}
     *         - 多租户关闭: system:role:perms
     */
    private String buildRolePermsCacheKey(Long tenantId) {
        // 判断是否启用多租户
        if (!tenantProperties.getEnabled() || tenantId == null) {
            // 单租户模式或多租户未开启：使用原有Key
            return RedisConstants.System.ROLE_PERMS;
        }
        // 多租户模式开启：Key按租户隔离
        return RedisConstants.System.ROLE_PERMS + ":" + tenantId;
    }

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
        
        if (tenantProperties.getEnabled()) {
            // 多租户模式：按租户分组缓存
            allRolePermsList.forEach(rolePerms -> {
                Long tenantId = rolePerms.getTenantId();
                if (tenantId == null) {
                    log.warn("多租户模式下，角色[{}]缺少tenantId，跳过", rolePerms.getRoleCode());
                    return;
                }
                String cacheKey = RedisConstants.System.ROLE_PERMS + ":" + tenantId;
                String roleCode = rolePerms.getRoleCode();
                Set<String> perms = rolePerms.getPerms();
                
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(cacheKey, roleCode, perms);
                }
            });
            log.info("权限缓存初始化完成（多租户模式），共{}条数据", allRolePermsList.size());
        } else {
            // 单租户模式：所有数据统一缓存
            String cacheKey = RedisConstants.System.ROLE_PERMS;
            allRolePermsList.forEach(rolePerms -> {
                String roleCode = rolePerms.getRoleCode();
                Set<String> perms = rolePerms.getPerms();
                
                if (CollectionUtil.isNotEmpty(perms)) {
                    redisTemplate.opsForHash().put(cacheKey, roleCode, perms);
                }
            });
            log.info("权限缓存初始化完成（单租户模式），共{}条数据", allRolePermsList.size());
        }
    }

    /**
     * 刷新当前租户权限缓存
     */
    @Override
    public void refreshRolePermsCache() {
        Long tenantId = TenantContextHolder.getTenantId();
        String cacheKey = buildRolePermsCacheKey(tenantId);
        
        // 清理当前租户权限缓存
        redisTemplate.delete(cacheKey);
        
        // 重新加载当前租户权限
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
        
        if (tenantId == null) {
            log.info("权限缓存刷新完成（单租户模式）");
        } else {
            log.info("租户[{}]权限缓存刷新完成", tenantId);
        }
    }

    /**
     * 刷新单个角色权限缓存
     */
    @Override
    public void refreshRolePermsCache(String roleCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        String cacheKey = buildRolePermsCacheKey(tenantId);
        
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
        
        if (tenantId == null) {
            log.info("角色[{}]权限缓存刷新完成（单租户模式）", roleCode);
        } else {
            log.info("租户[{}]角色[{}]权限缓存刷新完成", tenantId, roleCode);
        }
    }

    /**
     * 刷新权限缓存（角色编码变更时调用）
     */
    @Override
    public void refreshRolePermsCache(String oldRoleCode, String newRoleCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        String cacheKey = buildRolePermsCacheKey(tenantId);
        
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
        
        if (tenantId == null) {
            log.info("角色编码变更: {} -> {}，权限缓存已更新（单租户模式）", oldRoleCode, newRoleCode);
        } else {
            log.info("租户[{}]角色编码变更: {} -> {}，权限缓存已更新", tenantId, oldRoleCode, newRoleCode);
        }
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
