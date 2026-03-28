package com.youlai.boot.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.system.model.entity.SysUser;
import com.youlai.boot.system.model.vo.CurrentUserVO;
import com.youlai.boot.system.model.form.UserForm;
import com.youlai.boot.system.model.form.UserImportForm;
import com.youlai.boot.system.model.form.UserProfileForm;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 用户对象转换器
 *
 * @author Ray.Hao
 * @since 2022/6/8
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserForm toForm(SysUser entity);

    @InheritInverseConfiguration(name = "toForm")
    SysUser toEntity(UserForm entity);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    CurrentUserVO toCurrentUserVo(SysUser entity);

    SysUser toEntity(UserImportForm vo);

    SysUser toEntity(UserProfileForm formData);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id")
    })
    Option<String> toOption(SysUser entity);

    List<Option<String>> toOptions(List<SysUser> list);
}
