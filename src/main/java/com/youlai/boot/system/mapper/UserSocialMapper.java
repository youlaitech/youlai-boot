package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.system.model.entity.UserSocial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户第三方账号绑定持久层
 */
@Mapper
public interface UserSocialMapper extends BaseMapper<UserSocial> {

    /**
     * 根据用户ID获取认证信息
     *
     * @param userId 用户ID
     * @return 认证信息
     */
    SecurityUser getAuthInfoByUserId(Long userId);

}
