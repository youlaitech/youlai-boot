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
     * 获取用户可访问的租户列表
     * <p>
     * 通过用户名查询该用户在所有租户下的账户，返回可访问的租户列表
     * </p>
     *
     * @param userId 用户ID
     * @return 可访问的租户列表
     */
    List<TenantVO> getAccessibleTenants(Long userId);

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
     * 检查用户是否可以访问指定租户
     * <p>
     * 验证该用户名在目标租户下是否存在账户
     * </p>
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return true-可访问，false-不可访问
     */
    boolean canAccessTenant(Long userId, Long tenantId);
}
