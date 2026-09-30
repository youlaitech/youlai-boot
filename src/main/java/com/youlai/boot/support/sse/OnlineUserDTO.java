package com.youlai.boot.support.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户信息 DTO。
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用户名
    private String username;

    // 会话数量，多设备登录时大于 1
    private int sessionCount;

    // 最早登录时间（毫秒时间戳）
    private long loginTime;
}
