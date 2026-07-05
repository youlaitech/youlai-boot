package com.youlai.boot.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.youlai.boot.common.base.IBaseEnum;
import lombok.Getter;

/**
 * 第三方登录平台类型枚举
 */
@Getter
public enum SocialPlatformEnum implements IBaseEnum<String> {

    WECHAT_MINI("WECHAT_MINI", "微信小程序"),
    WECHAT_MP("WECHAT_MP", "微信公众号"),
    WECHAT_OPEN("WECHAT_OPEN", "微信开放平台"),
    ALIPAY("ALIPAY", "支付宝"),
    QQ("QQ", "QQ"),
    APPLE("APPLE", "Apple ID");

    @EnumValue
    private final String value;

    private final String label;

    SocialPlatformEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

}
