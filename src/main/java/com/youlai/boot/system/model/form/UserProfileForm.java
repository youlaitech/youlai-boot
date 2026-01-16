package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别")
    private Integer gender;

}
