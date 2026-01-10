package com.youlai.boot.system.enums;

import com.youlai.boot.common.base.IBaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 通告发布状态枚举
 *
 * @author Ray.Hao
 * @since 2024/10/14
 */
@Getter
@Schema(enumAsRef = true)
public enum NoticePublishStatusEnum implements IBaseEnum<Integer> {

    UNPUBLISHED(0, "未发布"),
    PUBLISHED(1, "已发布"),
    REVOKED(-1, "已撤回");


    private final Integer value;

    private final String label;

    NoticePublishStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
