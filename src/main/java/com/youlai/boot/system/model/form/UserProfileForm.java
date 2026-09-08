package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 个人中心用户信息
 *
 * @author Ray.Hao
 * @since 2024/8/13
 */
@Schema(description = "个人中心用户信息")
@Data
public class UserProfileForm {

    @Schema(description = "用户昵称")
    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 50, message = "用户昵称长度不能超过 50 个字符")
    private String nickname;

    @Schema(description = "头像URL")
    @Size(max = 255, message = "头像地址长度不能超过 255 个字符")
    private String avatar;

    @Schema(description = "性别")
    @Range(min = 0, max = 2, message = "性别取值不正确")
    private Integer gender;

}
