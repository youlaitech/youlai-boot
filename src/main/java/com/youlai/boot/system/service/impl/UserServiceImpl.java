package com.youlai.boot.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.platform.sms.enums.SmsTypeEnum;
import com.youlai.boot.platform.sms.service.SmsService;
import com.youlai.boot.security.model.UserAuthCredentials;
import com.youlai.boot.security.service.PermissionService;
import com.youlai.boot.security.token.TokenManager;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.platform.mail.service.MailService;
import com.youlai.boot.system.converter.UserConverter;
import com.youlai.boot.system.enums.DictCodeEnum;
import com.youlai.boot.system.mapper.UserMapper;
import com.youlai.boot.system.model.bo.UserBO;
import com.youlai.boot.system.model.dto.CurrentUserDTO;
import com.youlai.boot.system.model.dto.UserExportDTO;
import com.youlai.boot.system.model.entity.DictItem;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.model.entity.UserRole;
import com.youlai.boot.system.model.form.*;
import com.youlai.boot.system.model.query.UserPageQuery;
import com.youlai.boot.system.model.vo.UserPageVO;
import com.youlai.boot.system.model.vo.UserProfileVO;
import com.youlai.boot.system.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户业务实现类
 *
 * @author Ray.Hao
 * @since 2022/1/14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    private final SmsService smsService;

    private final MailService mailService;

    private final StringRedisTemplate redisTemplate;

    private final TokenManager tokenManager;

    private final DictItemService dictItemService;

    private final UserConverter userConverter;

    /**
     * 获取用户分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<UserPageVO>} 用户分页列表
     */
    @Override
    public IPage<UserPageVO> getUserPage(UserPageQuery queryParams) {

        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<UserBO> page = new Page<>(pageNum, pageSize);

        boolean isRoot = SecurityUtils.isRoot();
        queryParams.setIsRoot(isRoot);

        // 查询数据
        Page<UserBO> userPage = this.baseMapper.getUserPage(page, queryParams);

        // 实体转换
        return userConverter.toPageVo(userPage);
    }

    /**
     * 获取用户表单数据
     *
     * @param userId 用户ID
     * @return {@link UserForm} 用户表单数据
     */
    @Override
    public UserForm getUserFormData(Long userId) {
        return this.baseMapper.getUserFormData(userId);
    }

    /**
     * 新增用户
     *
     * @param userForm 用户表单对象
     * @return true|false
     */
    @Override
    public boolean saveUser(UserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        Assert.isTrue(count == 0, "用户名已存在");

        // 实体转换 form->entity
        User entity = userConverter.toEntity(userForm);

        // 设置默认加密密码
        String defaultEncryptPwd = passwordEncoder.encode(SystemConstants.DEFAULT_PASSWORD);
        entity.setPassword(defaultEncryptPwd);
        entity.setCreateBy(SecurityUtils.getUserId());

        // 新增用户
        boolean result = this.save(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 更新用户
     *
     * @param userId   用户ID
     * @param userForm 用户表单对象
     * @return true|false
     */
    @Override
    @Transactional
    public boolean updateUser(Long userId, UserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(User::getId, userId)
        );
        Assert.isTrue(count == 0, "用户名已存在");

        // form -> entity
        User entity = userConverter.toEntity(userForm);
        entity.setUpdateBy(SecurityUtils.getUserId());

        // 修改用户
        boolean result = this.updateById(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 删除用户
     *
     * @param idsStr 用户ID，多个以英文逗号(,)分割
     * @return true|false
     */
    @Override
    public boolean deleteUsers(String idsStr) {
        Assert.isTrue(StrUtil.isNotBlank(idsStr), "删除的用户数据为空");
        // 逻辑删除
        List<Long> ids = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return this.removeByIds(ids);

    }

    /**
     * 根据用户名获取认证凭证信息
     *
     * @param username 用户名
     * @return 用户认证凭证信息 {@link UserAuthCredentials}
     */
    @Override
    public UserAuthCredentials getAuthCredentialsByUsername(String username) {
        UserAuthCredentials userAuthCredentials = this.baseMapper.getAuthCredentialsByUsername(username);
        if (userAuthCredentials != null) {
            Set<String> roles = userAuthCredentials.getRoles();
            // 获取最大范围的数据权限
            Integer dataScope = roleService.getMaximumDataScope(roles);
            userAuthCredentials.setDataScope(dataScope);
        }
        return userAuthCredentials;
    }

    /**
     * 根据OpenID获取用户认证信息
     *
     * @param openId 微信OpenID
     * @return 用户认证信息
     */
    @Override
    public UserAuthCredentials getAuthCredentialsByOpenId(String openId) {
        if (StrUtil.isBlank(openId)) {
            return null;
        }
        UserAuthCredentials userAuthCredentials = this.baseMapper.getAuthCredentialsByOpenId(openId);
        if (userAuthCredentials != null) {
            Set<String> roles = userAuthCredentials.getRoles();
            // 获取最大范围的数据权限
            Integer dataScope = roleService.getMaximumDataScope(roles);
            userAuthCredentials.setDataScope(dataScope);
        }
        return userAuthCredentials;
    }

    /**
     * 根据手机号获取用户认证信息
     *
     * @param mobile 手机号
     * @return 用户认证信息
     */
    @Override
    public UserAuthCredentials getAuthCredentialsByMobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return null;
        }
        UserAuthCredentials userAuthCredentials = this.baseMapper.getAuthCredentialsByMobile(mobile);
        if (userAuthCredentials != null) {
            Set<String> roles = userAuthCredentials.getRoles();
            // 获取最大范围的数据权限
            Integer dataScope = roleService.getMaximumDataScope(roles);
            userAuthCredentials.setDataScope(dataScope);
        }
        return userAuthCredentials;
    }

    /**
     * 注册或绑定微信用户
     *
     * @param openId 微信OpenID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerOrBindWechatUser(String openId) {
        if (StrUtil.isBlank(openId)) {
            return false;
        }

        // 查询是否已存在该openId的用户
        User existUser = this.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openId)
        );

        if (existUser != null) {
            // 用户已存在，不需要注册
            return true;
        }

        // 创建新用户
        User newUser = new User();
        newUser.setNickname("微信用户");  // 默认昵称
        newUser.setUsername(openId);      // TODO 后续替换为手机号
        newUser.setOpenid(openId);
        newUser.setGender(0); // 保密
        newUser.setUpdateBy(SecurityUtils.getUserId());
        newUser.setPassword(SystemConstants.DEFAULT_PASSWORD);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        this.save(newUser);
        // 为了默认系统管理员角色，这里按需调整，实际情况绑定已存在的系统用户，另一种情况是给默认游客角色，然后由系统管理员设置用户的角色
        UserRole userRole = new UserRole();
        userRole.setUserId(newUser.getId());
        userRole.setRoleId(1L);  // TODO 系统管理员
        userRoleService.save(userRole);
        return true;
    }

    /**
     * 根据手机号和OpenID注册用户
     *
     * @param mobile 手机号
     * @param openId 微信OpenID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerUserByMobileAndOpenId(String mobile, String openId) {
        if (StrUtil.isBlank(mobile) || StrUtil.isBlank(openId)) {
            return false;
        }

        // 先查询是否已存在手机号对应的用户
        User existingUser = this.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getMobile, mobile)
        );

        if (existingUser != null) {
            // 如果存在用户但没绑定openId，则绑定openId
            if (StrUtil.isBlank(existingUser.getOpenid())) {
                return bindUserOpenId(existingUser.getId(), openId);
            }
            // 如果已经绑定了其他openId，则判断是否需要更新
            else if (!openId.equals(existingUser.getOpenid())) {
                return bindUserOpenId(existingUser.getId(), openId);
            }
            // 如果已经绑定了相同的openId，则不需要任何操作
            return true;
        }

        // 不存在用户，创建新用户
        User newUser = new User();
        newUser.setMobile(mobile);
        newUser.setOpenid(openId);
        newUser.setUsername(mobile); // 使用手机号作为用户名
        newUser.setNickname("微信用户_" + mobile.substring(mobile.length() - 4)); // 使用手机号后4位作为昵称
        newUser.setPassword(SystemConstants.DEFAULT_PASSWORD); // 使用加密的openId作为初始密码
        newUser.setGender(0); // 保密
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        this.save(newUser);
        // 为了默认系统管理员角色，这里按需调整，实际情况绑定已存在的系统用户，另一种情况是给默认游客角色，然后由系统管理员设置用户的角色
        UserRole userRole = new UserRole();
        userRole.setUserId(newUser.getId());
        userRole.setRoleId(1L);  // TODO 系统管理员
        userRoleService.save(userRole);
        return true;
    }

    /**
     * 绑定用户微信OpenID
     *
     * @param userId 用户ID
     * @param openId 微信OpenID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOpenId(Long userId, String openId) {
        if (userId == null || StrUtil.isBlank(openId)) {
            return false;
        }

        // 检查是否已有其他用户绑定了此openId
        User existingUser = this.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openId)
                        .ne(User::getId, userId)
        );

        if (existingUser != null) {
            log.warn("OpenID {} 已被用户 {} 绑定，无法为用户 {} 绑定", openId, existingUser.getId(), userId);
            return false;
        }

        // 更新用户openId
        boolean updated = this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getOpenid, openId)
                        .set(User::getUpdateTime, LocalDateTime.now())
        );
        return updated ;
    }

    /**
     * 获取导出用户列表
     *
     * @param queryParams 查询参数
     * @return {@link List<UserExportDTO>} 导出用户列表
     */
    @Override
    public List<UserExportDTO> listExportUsers(UserPageQuery queryParams) {

        boolean isRoot = SecurityUtils.isRoot();
        queryParams.setIsRoot(isRoot);

        List<UserExportDTO> exportUsers = this.baseMapper.listExportUsers(queryParams);
        if (CollectionUtil.isNotEmpty(exportUsers)) {
            //获取性别的字典项
            Map<String, String> genderMap = dictItemService.list(
                            new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictCode,
                                    DictCodeEnum.GENDER.getValue())
                    ).stream()
                    .collect(Collectors.toMap(DictItem::getValue, DictItem::getLabel)
                    );

            exportUsers.forEach(item -> {
                String gender = item.getGender();
                if (StrUtil.isBlank(gender)) {
                    return;
                }

                // 判断map是否为空
                if (genderMap.isEmpty()) {
                    return;
                }

                item.setGender(genderMap.get(gender));
            });
        }
        return exportUsers;
    }

    /**
     * 获取登录用户信息
     *
     * @return {@link CurrentUserDTO}   用户信息
     */
    @Override
    public CurrentUserDTO getCurrentUserInfo() {

        String username = SecurityUtils.getUsername();

        // 获取登录用户基础信息
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .select(
                        User::getId,
                        User::getUsername,
                        User::getNickname,
                        User::getAvatar
                )
        );
        // entity->VO
        CurrentUserDTO userInfoVO = userConverter.toCurrentUserDto(user);

        // 用户角色集合
        Set<String> roles = SecurityUtils.getRoles();
        userInfoVO.setRoles(roles);

        // 用户权限集合
        if (CollectionUtil.isNotEmpty(roles)) {
            Set<String> perms = permissionService.getRolePermsFormCache(roles);
            userInfoVO.setPerms(perms);
        }
        return userInfoVO;
    }

    /**
     * 获取个人中心用户信息
     *
     * @param userId 用户ID
     * @return {@link UserProfileVO} 个人中心用户信息
     */
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        UserBO entity = this.baseMapper.getUserProfile(userId);
        return userConverter.toProfileVo(entity);
    }

    /**
     * 修改个人中心用户信息
     *
     * @param formData 表单数据
     * @return true|false
     */
    @Override
    public boolean updateUserProfile(UserProfileForm formData) {
        Long userId = SecurityUtils.getUserId();
        User entity = userConverter.toEntity(formData);
        entity.setId(userId);
        return this.updateById(entity);
    }

    /**
     * 修改指定用户密码
     *
     * @param userId 用户ID
     * @param data   密码修改表单数据
     * @return true|false
     */
    @Override
    public boolean changeUserPassword(Long userId, PasswordUpdateForm data) {

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String oldPassword = data.getOldPassword();

        // 校验原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 新旧密码不能相同
        if (passwordEncoder.matches(data.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 判断新密码和确认密码是否一致
        if (passwordEncoder.matches(data.getNewPassword(), data.getConfirmPassword())) {
            throw new BusinessException("新密码和确认密码不一致");
        }

        String newPassword = data.getNewPassword();
        boolean result = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(newPassword))
        );

        if (result) {
            // 密码变更后，使当前用户的所有会话失效，强制重新登录
            tokenManager.invalidateUserSessions(userId);
        }
        return result;
    }

    /**
     * 重置指定用户密码
     *
     * @param userId   用户ID
     * @param password 密码重置表单数据
     * @return true|false
     */
    @Override
    public boolean resetUserPassword(Long userId, String password) {
        boolean result = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(password))
        );
        if (result) {
            // 管理员重置用户密码后，使该用户的所有会话失效
            tokenManager.invalidateUserSessions(userId);
        }
        return result;
    }

    /**
     * 发送短信验证码(绑定或更换手机号)
     *
     * @param mobile 手机号
     * @return true|false
     */
    @Override
    public boolean sendMobileCode(String mobile) {

        // String code = String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
        // TODO 为了方便测试，验证码固定为 1234，实际开发中在配置了厂商短信服务后，可以使用上面的随机验证码
        String code = "1234";

        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("code", code);
        boolean result = smsService.sendSms(mobile, SmsTypeEnum.CHANGE_MOBILE, templateParams);
        if (result) {
            // 缓存验证码，5分钟有效，用于更换手机号校验
            String redisCacheKey = StrUtil.format(RedisConstants.Captcha.MOBILE_CODE, mobile);
            redisTemplate.opsForValue().set(redisCacheKey, code, 5, TimeUnit.MINUTES);
        }
        return result;
    }

    /**
     * 绑定或更换手机号
     *
     * @param form 表单数据
     * @return true|false
     */
    @Override
    public boolean bindOrChangeMobile(MobileUpdateForm form) {

        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = this.getById(currentUserId);

        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验验证码
        String inputVerifyCode = form.getCode();
        String mobile = form.getMobile();

        String cacheKey = StrUtil.format(RedisConstants.Captcha.MOBILE_CODE, mobile);

        String cachedVerifyCode = redisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isBlank(cachedVerifyCode)) {
            throw new BusinessException("验证码已过期");
        }
        if (!inputVerifyCode.equals(cachedVerifyCode)) {
            throw new BusinessException("验证码错误");
        }
        // 验证完成删除验证码
        redisTemplate.delete(cacheKey);

        // 更新手机号码
        return this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, currentUserId)
                        .set(User::getMobile, mobile)
        );
    }

    /**
     * 发送邮箱验证码（绑定或更换邮箱）
     *
     * @param email 邮箱
     */
    @Override
    public void sendEmailCode(String email) {

        // String code = String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
        // TODO 为了方便测试，验证码固定为 1234，实际开发中在配置了邮箱服务后，可以使用上面的随机验证码
        String code = "1234";

        mailService.sendMail(email, "邮箱验证码", "您的验证码为：" + code + "，请在5分钟内使用");
        // 缓存验证码，5分钟有效，用于更换邮箱校验
        String redisCacheKey = StrUtil.format(RedisConstants.Captcha.EMAIL_CODE, email);
        redisTemplate.opsForValue().set(redisCacheKey, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 修改当前用户邮箱
     *
     * @param form 表单数据
     * @return true|false
     */
    @Override
    public boolean bindOrChangeEmail(EmailUpdateForm form) {

        Long currentUserId = SecurityUtils.getUserId();

        User currentUser = this.getById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 获取前端输入的验证码
        String inputVerifyCode = form.getCode();

        // 获取缓存的验证码
        String email = form.getEmail();
        String redisCacheKey = StrUtil.format(RedisConstants.Captcha.EMAIL_CODE, email);
        String cachedVerifyCode = redisTemplate.opsForValue().get(redisCacheKey);

        if (StrUtil.isBlank(cachedVerifyCode)) {
            throw new BusinessException("验证码已过期");
        }

        if (!inputVerifyCode.equals(cachedVerifyCode)) {
            throw new BusinessException("验证码错误");
        }
        // 验证完成删除验证码
        redisTemplate.delete(redisCacheKey);

        // 更新邮箱地址
        return this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, currentUserId)
                        .set(User::getEmail, email)
        );
    }

    /**
     * 获取用户选项列表
     *
     * @return {@link List<Option<String>>} 用户选项列表
     */
    @Override
    public List<Option<String>> listUserOptions() {
        List<User> list = this.list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
        );
        return userConverter.toOptions(list);
    }

}
