package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户通知公告实体对象
 *
 * @author Kylin
 * @since 2024-08-28 16:56
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_notice")
public class UserNotice extends BaseEntity {

    /** 公共通知id */
    private Long noticeId;
    /** 用户id */
    private Long userId;
    /** 读取状态，0未读，1已读 */
    private Integer isRead;
    /** 用户阅读时间 */
    private LocalDateTime readTime;
    /** 逻辑删除标识(0-未删除 1-已删除) */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}

