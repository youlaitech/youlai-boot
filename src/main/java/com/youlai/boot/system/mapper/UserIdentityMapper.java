package com.youlai.boot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.system.model.entity.UserIdentity;
import com.youlai.boot.system.model.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户外部身份绑定持久层
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {

    /**
     * 锁定本地用户行，使没有关联记录时的并发新增也能串行处理
     */
    @Select("SELECT id, status FROM sys_user WHERE id = #{userId} AND is_deleted = 0 FOR UPDATE")
    SysUser lockUser(@Param("userId") Long userId);

    /**
     * 根据用户ID获取认证信息
     *
     * @param userId 用户ID
     * @return 认证信息
     */
    SecurityUser getAuthInfoByUserId(Long userId);

}
