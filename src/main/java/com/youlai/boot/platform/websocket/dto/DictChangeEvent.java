package com.youlai.boot.platform.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典变更事件
 * <p>
 * 当字典数据发生变更时，通过 WebSocket 广播此事件通知前端清除缓存。
 * 前端收到通知后清除对应字典的本地缓存，下次使用时重新从服务端加载。
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
