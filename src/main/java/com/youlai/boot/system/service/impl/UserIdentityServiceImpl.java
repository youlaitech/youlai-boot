package com.youlai.boot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.enums.StatusEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.system.mapper.UserIdentityMapper;
import com.youlai.boot.system.mapper.UserMapper;
import com.youlai.boot.system.model.entity.SysUser;
import com.youlai.boot.system.model.entity.UserIdentity;
import com.youlai.boot.system.service.RoleService;
import com.youlai.boot.system.service.UserIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 外部账号关联实现
 *
 * <p>同一用户的写操作先锁用户行，解决“尚无关联记录可锁”的并发新增；
 * 不同用户抢占同一外部账号时，由数据库唯一键裁决，冲突不会转移原有归属
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Service
@RequiredArgsConstructor
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity>
        implements UserIdentityService {

    private final UserMapper userMapper;
    private final RoleService roleService;

    @Override
    public UserIdentity getIdentity(Long userId, String provider) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        return findIdentity(userId, provider);
    }

    @Override
    public Long findUserId(String provider, String providerAccountId) {
        UserIdentity identity = findByAccount(provider, providerAccountId);
        return identity == null ? null : identity.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindIdentity(Long userId, String provider, String providerAccountId) {
        writeIdentity(userId, provider, providerAccountId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setIdentity(Long userId, String provider, String providerAccountId) {
        return writeIdentity(userId, provider, providerAccountId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeIdentity(Long userId, String provider) {
        requireUser(userId);
        return remove(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getProvider, provider));
    }

    @Override
    public SecurityUser getAuthInfoByProviderAccount(String provider, String providerAccountId) {
        Long userId = findUserId(provider, providerAccountId);
        return userId == null ? null : getAuthInfoByUserId(userId);
    }

    @Override
    public SecurityUser getAuthInfoByUserId(Long userId) {
        SecurityUser user = baseMapper.getAuthInfoByUserId(userId);
        if (user != null) {
            // 外部登录和扫码登录沿用本地角色的数据权限，不能只装载角色编码
            user.setDataScopes(roleService.getRoleDataScopes(user.getRoles()));
        }
        return user;
    }

    @Override
    public void markLogin(String provider, String providerAccountId) {
        update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getProvider, provider)
                .eq(UserIdentity::getProviderAccountId, providerAccountId)
                .set(UserIdentity::getLastLoginTime, LocalDateTime.now()));
    }

    /**
     * replace 仅供明确的管理端设置操作使用；登录绑定不能隐式替换账号
     */
    private boolean writeIdentity(Long userId, String provider, String providerAccountId, boolean replace) {
        if (provider == null || !provider.matches("[a-z][a-z0-9-]{0,63}")
                || providerAccountId == null || providerAccountId.isBlank() || providerAccountId.length() > 255) {
            throw new BusinessException("认证源或外部账号标识不合法");
        }
        SysUser user = requireUser(userId);
        if (!StatusEnum.ENABLE.getValue().equals(user.getStatus())) {
            throw new BusinessException("用户已禁用，不能关联外部账号");
        }

        UserIdentity owner = findByAccount(provider, providerAccountId);
        if (owner != null && !Objects.equals(owner.getUserId(), userId)) {
            throw new BusinessException("该身份已关联其他用户");
        }

        UserIdentity current = findIdentity(userId, provider);
        if (current != null && current.getProviderAccountId().equals(providerAccountId)) {
            return false;
        }
        if (current != null && !replace) {
            throw new BusinessException("当前用户已关联该认证源的其他账号，请先解除关联");
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            if (current == null) {
                UserIdentity identity = new UserIdentity();
                identity.setUserId(userId);
                identity.setProvider(provider);
                identity.setProviderAccountId(providerAccountId);
                identity.setCreateTime(now);
                identity.setUpdateTime(now);
                save(identity);
            } else {
                // 外部账号改变后清空旧展示资料和登录时间，不能沿用上一个账号的信息
                update(new LambdaUpdateWrapper<UserIdentity>()
                        .eq(UserIdentity::getId, current.getId())
                        .set(UserIdentity::getProviderAccountId, providerAccountId)
                        .set(UserIdentity::getDisplayName, null)
                        .set(UserIdentity::getAvatarUrl, null)
                        .set(UserIdentity::getLastLoginTime, null)
                        .set(UserIdentity::getUpdateTime, now));
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("该身份已关联其他用户");
        }
        return true;
    }

    /**
     * 必须在事务中调用；锁保持到外层绑定或解除操作提交
     */
    private SysUser requireUser(Long userId) {
        SysUser user = baseMapper.lockUser(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserIdentity findIdentity(Long userId, String provider) {
        return getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getProvider, provider));
    }

    private UserIdentity findByAccount(String provider, String providerAccountId) {
        return getOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getProvider, provider)
                .eq(UserIdentity::getProviderAccountId, providerAccountId));
    }
}
