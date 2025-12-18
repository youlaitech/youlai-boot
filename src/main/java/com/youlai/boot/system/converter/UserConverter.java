package com.youlai.boot.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.model.dto.CurrentUserDto;
import com.youlai.boot.system.model.vo.UserPageVo;
import com.youlai.boot.system.model.vo.UserProfileVo;
import com.youlai.boot.system.model.bo.UserBo;
import com.youlai.boot.system.model.form.UserForm;
import com.youlai.boot.system.model.dto.UserImportDto;
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

    UserPageVo toPageVo(UserBo bo);

    Page<UserPageVo> toPageVo(Page<UserBo> bo);

    UserForm toForm(User entity);

    @InheritInverseConfiguration(name = "toForm")
    User toEntity(UserForm entity);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    CurrentUserDto toCurrentUserDto(User entity);

    User toEntity(UserImportDto vo);


    UserProfileVo toProfileVo(UserBo bo);

    User toEntity(UserProfileForm formData);

    @Mappings({
            @Mapping(target = "label", source = "nickname"),
            @Mapping(target = "value", source = "id")
    })
    Option<String> toOption(User entity);

    List<Option<String>> toOptions(List<User> list);
}
