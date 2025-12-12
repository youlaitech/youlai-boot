package com.youlai.boot.system.controller;

import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.system.model.vo.TenantVO;
import com.youlai.boot.system.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理控制器
 * <p>
 * 提供租户切换、查询等功能
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Tag(name = "租户管理接口")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TenantController {

    private final TenantService tenantService;

    /**
     * 获取当前用户的租户列表
     * <p>
     * 根据当前登录用户查询其所属的所有租户
     * </p>
     *
     * @return 租户列表
     */
    @Operation(summary = "获取当前用户可访问的租户列表")
    @GetMapping
    public Result<List<TenantVO>> getAccessibleTenants() {
        Long userId = SecurityUtils.getUserId();
        List<TenantVO> tenantList = tenantService.getAccessibleTenants(userId);
        log.debug("用户 {} 可访问 {} 个租户", userId, tenantList.size());
        return Result.success(tenantList);
    }

    /**
     * 获取当前租户信息
     *
     * @return 当前租户信息
     */
    @Operation(summary = "获取当前租户信息")
    @GetMapping("/current")
    public Result<TenantVO> getCurrentTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return Result.success(null);
        }
        TenantVO tenant = tenantService.getTenantById(tenantId);
        return Result.success(tenant);
    }

    /**
     * 切换租户
     * <p>
     * 切换当前用户的租户上下文，需要验证用户是否有权限访问该租户
     * </p>
     *
     * @param tenantId 目标租户ID
     * @return 切换结果
     */
    @Operation(summary = "切换租户")
    @PostMapping("/{tenantId}/switch")
    public Result<TenantVO> switchTenant(
            @Parameter(description = "租户ID") @PathVariable Long tenantId,
            HttpServletRequest request
    ) {
        Long userId = SecurityUtils.getUserId();
        Long fromTenantId = TenantContextHolder.getTenantId();
        
        log.info("用户 {} 请求切换租户：{} -> {}", userId, fromTenantId, tenantId);

        // 验证用户是否可以访问该租户
        if (!tenantService.canAccessTenant(userId, tenantId)) {
            log.warn("用户 {} 无权访问租户 {}", userId, tenantId);
            return Result.failed("无权访问该租户");
        }

        // 验证租户是否存在且正常
        TenantVO tenant = tenantService.getTenantById(tenantId);
        if (tenant == null) {
            log.warn("用户 {} 尝试切换到不存在的租户 {}", userId, tenantId);
            return Result.failed("租户不存在");
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            log.warn("用户 {} 尝试切换到已禁用的租户 {}", userId, tenantId);
            return Result.failed("租户已禁用");
        }

        // 设置新的租户上下文
        TenantContextHolder.setTenantId(tenantId);
        
        log.info("用户 {} 成功切换租户：{} -> {}", userId, fromTenantId, tenantId);

        return Result.success(tenant);
    }
}

