package com.youlai.boot.framework.security.service;

import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 系统用户认证 DetailsService。
 * <p>
 * 通过 {@link UserAuthenticationPort} 获取认证信息，不直接依赖 system 模块。
 *
 * @author Ray.Hao
 * @since 2021/10/19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserAuthenticationPort userAuthPort;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            SecurityUser securityUser = userAuthPort.getAuthInfoByUsername(username);
            if (securityUser == null) {
                throw new UsernameNotFoundException(username);
            }
            return new SecurityUserDetails(securityUser);
        } catch (Exception e) {
            log.error("认证异常:{}", e.getMessage());
            throw e;
        }
    }
}
