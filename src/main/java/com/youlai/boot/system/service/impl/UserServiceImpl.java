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
import com.youlai.boot.platform.mail.service.MailService;
import com.youlai.boot.platform.sms.enums.SmsTypeEnum;
import com.youlai.boot.platform.sms.service.SmsService;
import com.youlai.boot.security.model.RoleDataScope;
import com.youlai.boot.security.model.UserAuthInfo;
import com.youlai.boot.security.token.TokenManager;
import com.youlai.boot.security.util.SecurityUtils;
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
import com.youlai.boot.system.model.query.UserQuery;
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

    private final RoleMenuService roleMenuService;

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
    public IPage<UserPageVO> getUserPage(UserQuery queryParams) {

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
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUser(UserForm userForm) {

        String username = userForm.getUsername();
        
        // 实体转换 form->entity
        User entity = userConverter.toEntity(userForm);
        
        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        Assert.isTrue(count == 0, "用户名已存在");

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
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(Long userId, UserForm userForm) {

        String username = userForm.getUsername();
        
        // 获取原用户信息
        User oldUser = this.getById(userId);
        Assert.notNull(oldUser, "用户不存在");

        // 检查用户名是否已存在（排除当前用户）
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
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUsers(String idsStr) {
        Assert.isTrue(StrUtil.isNotBlank(idsStr), "删除的用户数据为空");
        // 逻辑删除
        List<Long> ids = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        
        boolean result = this.removeByIds(ids);
        return result;
    }

    /**
     * 根据用户名获取认证凭证信息
     *
     * @param username 用户名
     * @return 用户认证凭证信息 {@link UserAuthInfo}
     */
    @Override
    public UserAuthInfo getAuthInfoByUsername(String username) {
        UserAuthInfo userAuthInfo = this.baseMapper.getAuthInfoByUsername(username);
        if (userAuthInfo != null) {
            Set<String> roles = userAuthInfo.getRoles();
            // 获取数据权限列表（用于并集策略）
            List<RoleDataScope> dataScopes = roleService.getRoleDataScopes(roles);
            userAuthInfo.setDataScopes(dataScopes);
        }
        return userAuthInfo;
    }

    /**
     * 根据手机号获取用户认证信息
     *
     * @param mobile 手机号
     * @return 用户认证信息
     */
    @Override
    public UserAuthInfo getAuthInfoByMobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return null;
        }
        UserAuthInfo userAuthInfo = this.baseMapper.getAuthInfoByMobile(mobile);
        if (userAuthInfo != null) {
            Set<String> roles = userAuthInfo.getRoles();
            // 获取数据权限列表（用于并集策略）
            List<RoleDataScope> dataScopes = roleService.getRoleDataScopes(roles);
            userAuthInfo.setDataScopes(dataScopes);
        }
        return userAuthInfo;
    }

    /**
     * 获取导出用户列表
     *
     * @param queryParams 查询参数
     * @return {@link List<UserExportDTO>} 导出用户列表
     */
    @Override
    public List<UserExportDTO> listExportUsers(UserQuery queryParams) {

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
        // entity->Vo
        CurrentUserDTO userInfoVo = userConverter.toCurrentUserDto(user);

        // 用户角色集合
        Set<String> roles = SecurityUtils.getRoles();
        userInfoVo.setRoles(roles);

        // 用户权限集合
        if (CollectionUtil.isNotEmpty(roles)) {
            Set<String> perms = roleMenuService.getRolePermsByRoleCodes(roles);
            userInfoVo.setPerms(perms);
        }
        return userInfoVo;
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

        if (formData.getNickname() == null && formData.getAvatar() == null && formData.getGender() == null) {
            throw new BusinessException("请修改至少一个字段");
        }

        return this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(formData.getNickname() != null, User::getNickname, formData.getNickname())
                .set(formData.getAvatar() != null, User::getAvatar, formData.getAvatar())
                .set(formData.getGender() != null, User::getGender, formData.getGender())
        );
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
        if (!Objects.equals(data.getNewPassword(), data.getConfirmPassword())) {
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

        Long currentUserId = SecurityUtils.getUserId();
        long mobileCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getMobile, mobile)
                .ne(User::getId, currentUserId)
        );
        if (mobileCount > 0) {
            throw new BusinessException("手机号已被其他账号绑定");
        }

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

        if (!passwordEncoder.matches(form.getPassword(), currentUser.getPassword())) {
            throw new BusinessException("当前密码错误");
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

        long mobileCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getMobile, mobile)
                .ne(User::getId, currentUserId)
        );
        if (mobileCount > 0) {
            throw new BusinessException("手机号已被其他账号绑定");
        }

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

        Long currentUserId = SecurityUtils.getUserId();
        long emailCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getId, currentUserId)
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被其他账号绑定");
        }

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

        if (!passwordEncoder.matches(form.getPassword(), currentUser.getPassword())) {
            throw new BusinessException("当前密码错误");
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

        long emailCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getId, currentUserId)
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被其他账号绑定");
        }

        redisTemplate.delete(redisCacheKey);

        // 更新邮箱地址
        return this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, currentUserId)
                        .set(User::getEmail, email)
        );
    }

    /**
     * 解绑手机号
     *
     * @param form 表单数据
     * @return true|false
     */
    @Override
    public boolean unbindMobile(PasswordVerifyForm form) {

        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = this.getById(currentUserId);

        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        if (StrUtil.isBlank(currentUser.getMobile())) {
            throw new BusinessException("当前账号未绑定手机号");
        }

        if (!passwordEncoder.matches(form.getPassword(), currentUser.getPassword())) {
            throw new BusinessException("当前密码错误");
        }

        return this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .set(User::getMobile, null)
        );
    }

    /**
     * 解绑邮箱
     *
     * @param form 表单数据
     * @return true|false
     */
    @Override
    public boolean unbindEmail(PasswordVerifyForm form) {

        Long currentUserId = SecurityUtils.getUserId();
        User currentUser = this.getById(currentUserId);

        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        if (StrUtil.isBlank(currentUser.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱");
        }

        if (!passwordEncoder.matches(form.getPassword(), currentUser.getPassword())) {
            throw new BusinessException("当前密码错误");
        }

        return this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .set(User::getEmail, null)
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
