package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统日志 实体类
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模块
     */
    private LogModuleEnum module;

    /**
     * 操作类型
     */
    @TableField(value = "action_type")
    private ActionTypeEnum actionType;

    /**
     * 操作标题
     */
    private String title;

    /**
     * 自定义日志内容，最长 500 字符
     */
    private String content;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * 请求方式
     */
    @TableField(value = "request_method")
    private String requestMethod;

    /**
     * IP 地址
     */
    private String ip;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 设备
     */
    private String device;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 状态：0失败 1成功
     */
    private Integer status;

    /**
     * 错误信息，切面内截断至 1000 字符
     */
    @TableField(value = "error_msg")
    private String errorMsg;

    /**
     * 执行时间(毫秒)
     */
    private Integer executionTime;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;

}
