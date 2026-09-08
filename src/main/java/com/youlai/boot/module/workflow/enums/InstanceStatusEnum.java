package com.youlai.boot.module.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 流程实例状态枚举
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Getter
public enum InstanceStatusEnum {

    /**
     * 运行中
     */
    RUNNING("running", "运行中"),

    /**
     * 已完成（正常走完结束节点）
     */
    FINISHED("finished", "已完成"),

    /**
     * 已终止（被删除，含管理员终止与发起人撤销）
     */
    TERMINATED("terminated", "已终止");

    @JsonValue
    private final String value;

    private final String label;

    InstanceStatusEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

}
