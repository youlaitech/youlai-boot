package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.system.mapper.TenantMapper;
import com.youlai.boot.system.mapper.UserTenantMapper;
import com.youlai.boot.system.model.entity.Tenant;
import com.youlai.boot.system.model.entity.UserTenant;
import com.youlai.boot.system.model.vo.TenantVO;
import com.youlai.boot.system.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    private final UserTenantMapper userTenantMapper;

    @Override
    public List<TenantVO> getTenantListByUserId(Long userId) {
        // 临时忽略租户过滤，查询所有租户
        TenantContextHolder.setIgnoreTenant(true);
        try {
            // 查询用户关联的租户ID列表
            List<UserTenant> userTenants = userTenantMapper.selectList(
                    new LambdaQueryWrapper<UserTenant>()
                            .eq(UserTenant::getUserId, userId)
            );

            if (userTenants.isEmpty()) {
                return List.of();
            }

            // 提取租户ID列表
            List<Long> tenantIds = userTenants.stream()
                    .map(UserTenant::getTenantId)
                    .collect(Collectors.toList());

            // 查询租户信息
            List<Tenant> tenants = this.list(
                    new LambdaQueryWrapper<Tenant>()
                            .in(Tenant::getId, tenantIds)
                            .eq(Tenant::getStatus, 1) // 只查询正常状态的租户
                            .orderByDesc(Tenant::getId)
            );

            // 转换为VO并标记默认租户
            return tenants.stream().map(tenant -> {
                TenantVO vo = new TenantVO();
                BeanUtils.copyProperties(tenant, vo);
                // 查找是否为默认租户
                userTenants.stream()
                        .filter(ut -> ut.getTenantId().equals(tenant.getId()) && ut.getIsDefault() == 1)
                        .findFirst()
                        .ifPresent(ut -> vo.setIsDefault(true));
                return vo;
            }).collect(Collectors.toList());
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
    public boolean hasTenantPermission(Long userId, Long tenantId) {
        TenantContextHolder.setIgnoreTenant(true);
        try {
            UserTenant userTenant = userTenantMapper.selectOne(
                    new LambdaQueryWrapper<UserTenant>()
                            .eq(UserTenant::getUserId, userId)
                            .eq(UserTenant::getTenantId, tenantId)
                            .last("LIMIT 1")
            );
            return userTenant != null;
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }
}

