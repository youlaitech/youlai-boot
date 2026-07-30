package com.youlai.boot.system.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典变更事件
 * <p>
 * 当字典数据发生变更时，通过 SSE 广播此事件通知前端清除缓存。
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictChangeEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典编码 */
    private String dictCode;

    /** 事件时间戳 */
    private long timestamp;

    /**
     * 创建字典变更事件（自动设置当前时间戳）
     *
     * @param dictCode 字典编码
     */
    public DictChangeEvent(String dictCode) {
        this.dictCode = dictCode;
        this.timestamp = System.currentTimeMillis();
    }
}
