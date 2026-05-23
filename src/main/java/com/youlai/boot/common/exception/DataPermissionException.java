package com.youlai.boot.common.exception;

import lombok.Getter;

/**
 * 数据权限异常
 * <p>
 * 当数据权限拦截器拼接SQL条件失败时抛出，属于系统级异常，非业务异常。
 *
 * @author zc
 * @since 2.0.0
 */
@Getter
public class DataPermissionException extends RuntimeException {

    private final String mappedStatementId;

    public DataPermissionException(String mappedStatementId, String message) {
        super(message);
        this.mappedStatementId = mappedStatementId;
    }

    public DataPermissionException(String mappedStatementId, String message, Throwable cause) {
        super(message, cause);
        this.mappedStatementId = mappedStatementId;
    }

}
