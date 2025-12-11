package com.youlai.boot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.system.model.entity.Tenant;
import com.youlai.boot.system.model.vo.TenantVO;

import java.util.List;

/**
 * 租户服务接口
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
public interface TenantService extends IService<Tenant> {

    /**
     * 根据用户ID查询用户所属的租户列表
     *
     * @param userId 用户ID
     * @return 租户列表
     */
    List<TenantVO> getTenantListByUserId(Long userId);

    /**
     * 根据租户ID查询租户信息
     *
     * @param tenantId 租户ID
     * @return 租户信息
     */
    TenantVO getTenantById(Long tenantId);

    /**
     * 根据域名查询租户ID
     *
     * @param domain 域名
     * @return 租户ID
     */
    Long getTenantIdByDomain(String domain);

    /**
     * 验证用户是否有权限访问指定租户
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return true-有权限，false-无权限
     */
    boolean hasTenantPermission(Long userId, Long tenantId);

    /**
     * 记录租户切换审计日志
     *
     * @param userId       用户ID
     * @param fromTenantId 原租户ID
     * @param toTenantId   目标租户ID
     * @param success      是否成功
     * @param failReason   失败原因
     * @param request      HTTP请求对象
     */
    void recordTenantSwitch(Long userId, Long fromTenantId, Long toTenantId, 
                            boolean success, String failReason, jakarta.servlet.http.HttpServletRequest request);
}
