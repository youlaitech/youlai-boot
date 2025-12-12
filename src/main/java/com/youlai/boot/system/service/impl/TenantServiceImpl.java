package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.system.mapper.TenantMapper;
import com.youlai.boot.system.mapper.UserMapper;
import com.youlai.boot.system.model.entity.Tenant;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.model.vo.TenantVO;
import com.youlai.boot.system.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 租户服务实现类
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    private final UserMapper userMapper;

    @Override
    public List<TenantVO> getAccessibleTenants(Long userId) {
        // 临时忽略租户过滤，查询所有租户
        TenantContextHolder.setIgnoreTenant(true);
        try {
            // 先根据用户ID查询用户信息（获取 username）
            User user = userMapper.selectById(userId);
            if (user == null) {
                return List.of();
            }

            // 通过 username 查询该用户在所有租户下的记录，获取租户ID列表
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, user.getUsername())
                            .eq(User::getIsDeleted, 0)
            );

            if (users.isEmpty()) {
                return List.of();
            }

            // 提取租户ID列表（去重）
            List<Long> tenantIds = users.stream()
                    .map(User::getTenantId)
                    .filter(tenantId -> tenantId != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (tenantIds.isEmpty()) {
                return List.of();
            }

            // 查询租户信息
            List<Tenant> tenants = this.list(
                    new LambdaQueryWrapper<Tenant>()
                            .in(Tenant::getId, tenantIds)
                            .eq(Tenant::getStatus, 1) // 只查询正常状态的租户
                            .orderByDesc(Tenant::getId)
            );

            // 转换为VO，第一个租户作为默认租户
            return IntStream.range(0, tenants.size())
                    .mapToObj(index -> {
                        Tenant tenant = tenants.get(index);
                        TenantVO vo = new TenantVO();
                        BeanUtils.copyProperties(tenant, vo);
                        // 第一个租户作为默认租户
                        if (index == 0) {
                            vo.setIsDefault(true);
                        }
                        return vo;
                    })
                    .collect(Collectors.toList());
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }

    @Override
    public TenantVO getTenantById(Long tenantId) {
        TenantContextHolder.setIgnoreTenant(true);
        try {
            Tenant tenant = this.getById(tenantId);
            if (tenant == null) {
                return null;
            }
            TenantVO vo = new TenantVO();
            BeanUtils.copyProperties(tenant, vo);
            return vo;
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }

    @Override
    public Long getTenantIdByDomain(String domain) {
        TenantContextHolder.setIgnoreTenant(true);
        try {
            Tenant tenant = this.getOne(
                    new LambdaQueryWrapper<Tenant>()
                            .eq(Tenant::getDomain, domain)
                            .eq(Tenant::getStatus, 1)
                            .last("LIMIT 1")
            );
            return tenant != null ? tenant.getId() : null;
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }

    @Override
    public boolean canAccessTenant(Long userId, Long tenantId) {
        TenantContextHolder.setIgnoreTenant(true);
        try {
            // 先根据用户ID查询用户信息（获取 username）
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }

            // 检查该 username 在指定租户下是否存在用户记录
            User tenantUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, user.getUsername())
                            .eq(User::getTenantId, tenantId)
                            .eq(User::getIsDeleted, 0)
                            .last("LIMIT 1")
            );
            return tenantUser != null;
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }

}

