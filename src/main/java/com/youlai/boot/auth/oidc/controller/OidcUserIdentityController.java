package com.youlai.boot.auth.oidc.controller;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.auth.oidc.model.form.OidcUserIdentityForm;
import com.youlai.boot.auth.oidc.model.vo.OidcUserIdentityVO;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.session.SessionService;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.system.model.entity.UserIdentity;
import com.youlai.boot.system.service.UserIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 当前用户与认证中心的身份关联
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Tag(name = "02.用户接口")
@Validated
@RestController
@ConditionalOnOidcEnabled
@RequestMapping("/api/v1/users/{userId}/oidc-identity")
@RequiredArgsConstructor
public class OidcUserIdentityController {

    private final OidcProperties oidcProperties;
    private final UserIdentityService userIdentityService;
    private final SessionService sessionService;

    /**
     * 查询当前用户在当前认证源下的身份关联
     */
    @Operation(summary = "查看统一认证身份")
    @GetMapping
    public Result<OidcUserIdentityVO> getIdentity(
            @Parameter(description = "本地用户 ID") @PathVariable @Positive Long userId
    ) {
        requireSelf(userId);
        UserIdentity identity = userIdentityService.getIdentity(userId, oidcProperties.getProvider());
        return Result.success(new OidcUserIdentityVO(
                oidcProperties.getIssuer(), identity == null ? null : identity.getProviderAccountId()));
    }

    /**
     * 保存当前用户的身份关联，拒绝占用其他用户的身份
     */
    @Operation(summary = "设置统一认证身份")
    @PutMapping
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.UPDATE)
    public Result<Void> setIdentity(
            @Parameter(description = "本地用户 ID") @PathVariable @Positive Long userId,
            @RequestBody @Valid OidcUserIdentityForm form
    ) {
        requireSelf(userId);
        if (userIdentityService.setIdentity(userId, oidcProperties.getProvider(), form.getSubject())) {
            // 关联改变后撤销该用户的现有会话；相同 subject 重复保存不触发下线
            sessionService.revokeUserSessions(userId);
        }
        return Result.success();
    }

    /**
     * 解除当前用户在当前认证源下的身份关联
     */
    @Operation(summary = "解除统一认证身份")
    @DeleteMapping
    @Log(module = LogModuleEnum.USER, value = ActionTypeEnum.DELETE)
    public Result<Void> removeIdentity(
            @Parameter(description = "本地用户 ID") @PathVariable @Positive Long userId
    ) {
        requireSelf(userId);
        if (userIdentityService.removeIdentity(userId, oidcProperties.getProvider())) {
            sessionService.revokeUserSessions(userId);
        }
        return Result.success();
    }

    /**
     * 只允许操作当前登录用户自己的身份关联
     */
    private void requireSelf(Long userId) {
        if (!Objects.equals(userId, SecurityUtils.getUserId())) {
            throw new BusinessException("只能操作自己的统一认证身份");
        }
    }
}
