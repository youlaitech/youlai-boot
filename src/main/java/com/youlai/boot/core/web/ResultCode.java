package com.youlai.boot.core.web;

import java.io.Serializable;

/**
 * 响应码枚举
 * <p>
 * 参考《阿里巴巴 Java 开发手册》错误码设计建议：
 * 00000 表示成功。
 * A**** 表示用户端错误（如参数错误、认证失败等）。
 * B**** 表示当前系统执行出错（如系统超时等）。
 * C**** 表示调用第三方服务出错（如中间件、数据库等外部依赖）。
 * <p>
 * 错误码位数与号段说明：
 * - 错误码为字符串类型，共 5 位：错误产生来源（A/B/C） + 四位数字编号。
 * - 四位数字编号范围 0001~9999，大类之间建议按步长 100 预留号段（如 A0200、A0300、A0400）。
 * - 错误码后三位编号与 HTTP 状态码无关。
 * <p>
 * 说明：
 * - 本项目仅保留实际使用的错误码，并在 A/B/C 各保留少量示例，避免枚举无限膨胀。
 * - 如需扩展业务错误码，建议在对应宏观分类下按场景划分号段并保持全局唯一。
 * <p>
 * 附表（节选）：错误码列表（示例/项目使用项）
 * <pre>
 * | 错误码 | 中文描述             | 说明             |
 * |-------|----------------------|------------------|
 * | 00000 | 成功                 | 正常执行后的返回 |
 * | A0001 | 用户端错误           | 一级宏观错误码   |
 * | A0100 | 用户注册错误         | 二级宏观错误码   |
 * | A0101 | 用户未同意隐私协议   | 二级宏观错误码   |
 * | A0200 | 用户登录异常         | 二级宏观错误码   |
 * | A0201 | 用户账户不存在       | 二级宏观错误码   |
 * | A0202 | 用户账户被冻结       | 二级宏观错误码   |
 * | A0230 | 访问令牌无效或已过期 | 令牌校验失败     |
 * | A0241 | 用户验证码尝试次数超限 | 二级宏观错误码   |
 * | A0300 | 访问权限异常         | 二级宏观错误码   |
 * | A0301 | 访问未授权           | 二级宏观错误码   |
 * | A0400 | 用户请求参数错误     | 二级宏观错误码   |
 * | A0410 | 请求必填参数为空     | 二级宏观错误码   |
 * | A0500 | 用户请求服务异常     | 二级宏观错误码   |
 * | A0502 | 请求并发数超出限制   | 二级宏观错误码   |
 * | A0506 | 请勿重复提交         | 二级宏观错误码   |
 * | B0001 | 系统执行出错         | 一级宏观错误码   |
 * | B0100 | 系统执行超时         | 二级宏观错误码   |
 * | C0001 | 调用第三方服务出错   | 一级宏观错误码   |
 * | C0113 | 接口不存在           | 二级宏观错误码   |
 * | C0300 | 数据库服务出错       | 二级宏观错误码   |
 * </pre>
 *
 * @author Ray.Hao
 * @since 2020/6/23
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
