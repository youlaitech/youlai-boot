package com.youlai.boot.platform.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户信息DTO
 * <p>
 * 用于返回在线用户的基本信息，包括用户名、会话数量和登录时间。
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

    /** 用户名 */
    private String username;

    /** 会话数量（多设备登录时大于1） */
    private int sessionCount;

    /** 最早登录时间 */
    private long loginTime;
}
