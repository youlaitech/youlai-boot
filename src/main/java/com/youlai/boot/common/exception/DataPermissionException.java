package com.youlai.boot.common.exception;

import lombok.Getter;

/**
 * 数据权限异常
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
