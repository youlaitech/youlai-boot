package com.youlai.boot.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.system.mapper.TenantMapper;
import com.youlai.boot.system.mapper.TenantSwitchLogMapper;
import com.youlai.boot.system.mapper.UserMapper;
import com.youlai.boot.system.mapper.UserTenantMapper;
import com.youlai.boot.system.model.entity.Tenant;
import com.youlai.boot.system.model.entity.TenantSwitchLog;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.model.entity.UserTenant;
import com.youlai.boot.system.model.vo.TenantVO;
import com.youlai.boot.system.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final TenantSwitchLogMapper tenantSwitchLogMapper;
    private final UserMapper userMapper;

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

    @Override
    public void recordTenantSwitch(Long userId, Long fromTenantId, Long toTenantId, 
                                   boolean success, String failReason, HttpServletRequest request) {
        try {
            // 临时忽略租户过滤，确保日志可以写入
            TenantContextHolder.setIgnoreTenant(true);

            // 创建审计日志
            TenantSwitchLog log = new TenantSwitchLog();
            log.setUserId(userId);
            log.setFromTenantId(fromTenantId);
            log.setToTenantId(toTenantId);
            log.setSwitchTime(LocalDateTime.now());
            log.setStatus(success ? 1 : 0);
            log.setFailReason(failReason);

            // 获取用户名
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    log.setUsername(user.getUsername());
                }
            }

            // 获取租户名称
            if (fromTenantId != null) {
                Tenant fromTenant = this.getById(fromTenantId);
                if (fromTenant != null) {
                    log.setFromTenantName(fromTenant.getName());
                }
            }
            if (toTenantId != null) {
                Tenant toTenant = this.getById(toTenantId);
                if (toTenant != null) {
                    log.setToTenantName(toTenant.getName());
                }
            }

            // 获取IP地址和User-Agent
            if (request != null) {
                log.setIpAddress(getIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }

            // 保存审计日志
            tenantSwitchLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败不应影响业务，仅记录错误
            Slf4j.getLogger(this.getClass()).error("记录租户切换日志失败", e);
        } finally {
            TenantContextHolder.setIgnoreTenant(false);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

