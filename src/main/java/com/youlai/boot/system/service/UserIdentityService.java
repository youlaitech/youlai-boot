package com.youlai.boot.system.service;

import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.system.model.entity.UserIdentity;

/**
 * 外部账号关联服务；只处理已确认的身份，不负责认证、注册或角色分配
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public interface UserIdentityService {

    /**
     * 查询用户在指定认证源下的关联；用户不存在时报错，未关联时返回 null
     */
    UserIdentity getIdentity(Long userId, String provider);

    /**
     * 按认证源和来源方账号 ID 查找本地用户；未关联时返回 null
     */
    Long findUserId(String provider, String providerAccountId);

    /**
     * 首次关联外部账号；同一关联重复提交可成功，不覆盖任何已有账号归属
     */
    void bindIdentity(Long userId, String provider, String providerAccountId);

    /**
     * 设置用户在指定认证源下的账号，可替换当前用户的旧关联
     *
     * <p>其他用户已关联的账号不能转移。按本地用户加行锁，并由两个唯一键兜底并发冲突
     *
     * @return 关联是否发生变化；相同账号重复保存返回 false
     */
    boolean setIdentity(Long userId, String provider, String providerAccountId);

    /**
     * 解除指定用户、指定认证源的关联；重复解除返回 false
     */
    boolean removeIdentity(Long userId, String provider);

    /**
     * 装载外部账号对应的本地认证信息；未关联或本地用户已删除时返回 null
     */
    SecurityUser getAuthInfoByProviderAccount(String provider, String providerAccountId);

    /**
     * 按本地用户 ID 装载认证信息及角色数据权限，供票据兑换和扫码登录使用
     */
    SecurityUser getAuthInfoByUserId(Long userId);

    /**
     * 只更新时间，不改变账号所属用户
     */
    void markLogin(String provider, String providerAccountId);
}
