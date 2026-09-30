package com.youlai.boot.common.result;

import java.io.Serializable;

/**
 * 响应码枚举
 *
 * @author Ray.Hao
 * @since 4.3.1
 **/
public enum ResultCode implements IResultCode, Serializable {

    SUCCESS("00000", "成功"),
    
    /** 一级宏观错误码：用户端错误（由客户端输入/认证/权限/请求方式等引起，需客户端配合修正） */
    USER_ERROR("A0001", "用户端错误"),
    

    /** 二级宏观错误码：用户端具体错误（按号段细分，便于定位是注册/登录/令牌/参数/防重等问题） */

    /** A01xx：用户注册错误 */
    USER_REGISTRATION_ERROR("A0100", "用户注册错误"),
    USER_NOT_AGREE_PRIVACY_AGREEMENT("A0101", "用户未同意隐私协议"),

    /** A013x：校验码输入错误 */
    VERIFICATION_CODE_INPUT_ERROR("A0130", "校验码输入错误"),

    /** A02xx：用户登录异常 */
    USER_LOGIN_EXCEPTION("A0200", "用户登录异常"),
    ACCOUNT_NOT_FOUND("A0201", "用户账户不存在"),
    ACCOUNT_FROZEN("A0202", "用户账户被冻结"),
    USER_PASSWORD_ERROR("A0210", "用户名或密码错误"),
    /** A023x：令牌无效或已过期 */
    ACCESS_TOKEN_INVALID("A0230", "访问令牌无效或已过期"),
    REFRESH_TOKEN_INVALID("A0231", "刷新令牌无效或已过期"),
    /** A024x：验证码错误 */
    USER_VERIFICATION_CODE_ERROR("A0240", "验证码错误"),
    USER_VERIFICATION_CODE_ATTEMPT_LIMIT_EXCEEDED("A0241", "用户验证码尝试次数超限"),
    USER_VERIFICATION_CODE_EXPIRED("A0242", "用户验证码过期"),

    /** A025x：扫码登录异常 */
    QR_CODE_NOT_FOUND("A0250", "扫码登录票据不存在或已过期"),
    QR_CODE_STATUS_ILLEGAL("A0251", "当前扫码状态不允许该操作"),
    QR_CODE_USER_MISMATCH("A0252", "操作用户与扫码用户不一致"),
    QR_CODE_ALREADY_USED("A0253", "扫码登录票据已被使用"),

    /** A03xx：访问权限异常 */
    ACCESS_PERMISSION_EXCEPTION("A0300", "访问权限异常"),
    ACCESS_UNAUTHORIZED("A0301", "访问未授权"),

    /** A04xx：用户请求参数错误 */
    USER_REQUEST_PARAMETER_ERROR("A0400", "用户请求参数错误"),
    INVALID_USER_INPUT("A0402", "无效的用户输入"),
    REQUEST_REQUIRED_PARAMETER_IS_EMPTY("A0410", "请求必填参数为空"),
    PARAMETER_FORMAT_MISMATCH("A0421", "参数格式不匹配"),

    /** A05xx：用户请求服务异常 */
    USER_REQUEST_SERVICE_EXCEPTION("A0500", "用户请求服务异常"),
    REQUEST_CONCURRENCY_LIMIT_EXCEEDED("A0502", "请求并发数超出限制"),
    DUPLICATE_SUBMISSION("A0506", "请勿重复提交"),

    /** A07xx：文件处理异常 */
    UPLOAD_FILE_EXCEPTION("A0700", "上传文件异常"),
    DELETE_FILE_EXCEPTION("A0710", "删除文件异常"),
    
    /** 一级宏观错误码：系统端错误（服务端内部异常/超时/不可用等，需后端排查修复） */
    SYSTEM_ERROR("B0001", "系统执行出错"),

    /** 二级宏观错误码：系统端具体错误（按号段细分，便于定位超时/限流/资源耗尽等） */
    SYSTEM_EXECUTION_TIMEOUT("B0100", "系统执行超时"),

    /** 一级宏观错误码：第三方服务错误（外部依赖/中间件/数据库等引起，需检查依赖健康与配置） */
    THIRD_PARTY_SERVICE_ERROR("C0001", "调用第三方服务出错"),

    /** 二级宏观错误码：第三方服务具体错误（按号段细分，便于定位是接口不存在/数据库异常等） */
    INTERFACE_NOT_EXIST("C0113", "接口不存在"),
    DATABASE_SERVICE_ERROR("C0300", "数据库服务出错"),
    DATABASE_EXECUTION_ERROR("C0310", "数据库执行异常"),
    DATABASE_EXECUTION_SYNTAX_ERROR("C0313", "数据库执行语法错误"),
    INTEGRITY_CONSTRAINT_VIOLATION("C0342", "违反了完整性约束"),
    DATABASE_ACCESS_DENIED("C0351", "演示环境已禁用数据库写入功能，请本地部署修改数据库链接或开启Mock模式进行体验");

    private final String code;

    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "{" +
                "\"code\":\"" + code + '\"' +
                ", \"msg\":\"" + msg + '\"' +
                '}';
    }


    public static ResultCode getValue(String code) {
        for (ResultCode value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return SYSTEM_ERROR; // 默认系统执行错误
    }
}
