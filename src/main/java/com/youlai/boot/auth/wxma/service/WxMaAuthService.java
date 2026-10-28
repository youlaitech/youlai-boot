package com.youlai.boot.auth.wxma.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.wxma.exception.MobileNotBoundException;
import com.youlai.boot.auth.wxma.config.WxMaProperties;
import com.youlai.boot.auth.wxma.model.vo.WxMaLoginVO;
import com.youlai.boot.auth.wxma.token.WxMaAuthenticationToken;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.session.SessionService;
import com.youlai.boot.system.model.entity.SysUser;
import com.youlai.boot.system.service.UserIdentityService;
import com.youlai.boot.system.service.UserRoleService;
import com.youlai.boot.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序认证服务
 *
 * @author Ray.Hao
 * @since 4.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WxMaAuthService {

    private final WxMaService wxMaService;
    private final WxMaProperties wxMaProperties;
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;
    private final UserService userService;
    private final UserIdentityService userIdentityService;
    private final UserRoleService userRoleService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 静默登录
     *
     * @param code 微信登录凭证（wx.login 获取）
     * @return 已关联时返回令牌，否则返回需绑定手机号的标记
     */
    public WxMaLoginVO silentLogin(String code) {
        WxMaAuthenticationToken token = new WxMaAuthenticationToken(code);

        try {
            Authentication authentication = authenticationManager.authenticate(token);
            TokenPair authToken = sessionService.create(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return WxMaLoginVO.builder()
                    .isNewUser(false)
                    .needBindMobile(false)
                    .accessToken(authToken.accessToken())
                    .refreshToken(authToken.refreshToken())
                    .tokenType(authToken.tokenType())
                    .expiresIn(authToken.expiresIn())
                    .build();
        } catch (MobileNotBoundException e) {
            return WxMaLoginVO.builder()
                    .isNewUser(true)
                    .needBindMobile(true)
                    .build();
        }
    }

    /**
     * 手机号快捷登录
     *
     * @param loginCode 微信登录凭证（wx.login 获取）
     * @param phoneCode 手机号授权凭证（getPhoneNumber 事件获取）
     * @return 认证令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenPair phoneLogin(String loginCode, String phoneCode) {
        // 1. 解析微信登录凭证，获取会话信息
        WxMaJscode2SessionResult session = resolveSession(loginCode);
        String openid = session.getOpenid();

        // 2. 解析手机号授权凭证，获取手机号
        String mobile = resolvePhoneNumber(phoneCode);

        log.info("微信小程序手机号快捷登录：openid={}, mobile={}", openid, mobile);

        // 3. 查询或创建用户
        SysUser user = findOrCreateUser(mobile);

        // 4. 绑定微信 openid
        bindWechatOpenid(user, session);

        // 5. 生成认证令牌
        return generateAuthToken(mobile);
    }

    /**
     * 绑定手机号
     *
     * @param loginCode 提交绑定前重新获取的微信一次性登录凭证
     * @param mobile  手机号码
     * @param smsCode 短信验证码
     * @return 认证令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenPair bindMobile(String loginCode, String mobile, String smsCode) {
        // OpenID 只从微信接口取得，不能把前端提交的一串 ID 当成身份确认
        WxMaJscode2SessionResult session = resolveSession(loginCode);
        // 1. 验证短信验证码
        validateSmsCode(mobile, smsCode);

        // 2. 查询或创建用户
        SysUser user = findOrCreateUser(mobile);

        // 3. 绑定微信 openid
        bindWechatOpenid(user, session);

        log.info("微信小程序绑定手机号成功：userId={}", user.getId());

        // 4. 生成认证令牌
        return generateAuthToken(mobile);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析微信登录凭证，获取会话信息
     */
    private WxMaJscode2SessionResult resolveSession(String loginCode) {
        try {
            return wxMaService.jsCode2SessionInfo(loginCode);
        } catch (Exception e) {
            log.error("获取微信会话信息失败", e);
            throw new IllegalArgumentException("微信登录失败：" + e.getMessage());
        }
    }

    /**
     * 解析手机号授权凭证，获取手机号
     */
    private String resolvePhoneNumber(String phoneCode) {
        try {
            WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService().getPhoneNoInfo(phoneCode);
            return phoneInfo.getPhoneNumber();
        } catch (Exception e) {
            log.error("获取微信手机号失败", e);
            throw new IllegalArgumentException("获取手机号失败：" + e.getMessage());
        }
    }

    /**
     * 查询或创建用户
     */
    private SysUser findOrCreateUser(String mobile) {
        SysUser user = userService.lambdaQuery()
                .eq(SysUser::getMobile, mobile)
                .one();

        if (user == null) {
            user = createNewUser(mobile);
            log.info("微信小程序登录：创建新用户，mobile={}, userId={}", mobile, user.getId());
        }

        return user;
    }

    /**
     * 创建新用户
     */
    private SysUser createNewUser(String mobile) {
        SysUser user = new SysUser();
        user.setMobile(mobile);
        user.setUsername("wx_" + IdUtil.fastSimpleUUID().substring(0, 8));
        user.setNickname("微信用户");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.save(user);

        // 分配 GUEST 角色（角色ID=3）
        userRoleService.replaceUserRoles(user.getId(), Collections.singletonList(3L));

        return user;
    }

    /**
     * 绑定微信 openid
     */
    private void bindWechatOpenid(SysUser user, WxMaJscode2SessionResult session) {
        // 冲突必须回滚本次用户创建和关联，不能吞掉失败后继续签发令牌
        userIdentityService.bindIdentity(user.getId(), wxMaProperties.getProvider(), session.getOpenid());
        cacheSessionKey(session.getOpenid(), session.getSessionKey());
        userIdentityService.markLogin(wxMaProperties.getProvider(), session.getOpenid());
    }

    /**
     * 缓存微信会话密钥，供后续解密类接口使用；数据库不再保存该凭证
     */
    private void cacheSessionKey(String openid, String sessionKey) {
        if (StrUtil.isBlank(openid) || StrUtil.isBlank(sessionKey)) {
            return;
        }
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.WxMa.SESSION_KEY, wxMaProperties.getProvider(), openid),
                sessionKey,
                RedisConstants.WxMa.SESSION_KEY_TTL_DAYS,
                TimeUnit.DAYS);
    }

    /**
     * 验证短信验证码
     */
    private void validateSmsCode(String mobile, String smsCode) {
        String cacheKey = StrUtil.format(RedisConstants.Captcha.SMS_LOGIN_CODE, mobile);
        String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);

        if (!StrUtil.equals(smsCode, cachedCode)) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 验证成功后删除验证码
        redisTemplate.delete(cacheKey);
    }

    /**
     * 生成认证令牌
     */
    private TokenPair generateAuthToken(String mobile) {
        SecurityUserDetails userDetails = new SecurityUserDetails(userService.getAuthInfoByMobile(mobile));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        TokenPair authToken = sessionService.create(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authToken;
    }
}
