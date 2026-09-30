package com.youlai.boot.system.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典变更通知数据
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictChangeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 字典编码
    private String dictCode;

    // 变更时间戳
    private long timestamp;

    /**
     * 创建字典变更通知数据（自动设置当前时间戳）
     *
     * @param dictCode 字典编码
     */
    public DictChangeDTO(String dictCode) {
        this.dictCode = dictCode;
        this.timestamp = System.currentTimeMillis();
    }
}