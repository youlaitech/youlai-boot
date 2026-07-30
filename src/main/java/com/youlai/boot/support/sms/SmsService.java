package com.youlai.boot.support.sms;

import java.util.Map;

/**
 * 短信发送服务接口。
 * <p>
 * 由各短信供应商实现（阿里云、腾讯云等），通过 {@code sms.type} 配置项选择。
 *
 * @author Ray.Hao
 * @since 2024/8/17
 */
public interface SmsService {

    /**
     * 发送短信。
     *
     * @param mobile         手机号
     * @param smsType        短信类型，对应 {@code sms.templates.*} 配置中的模板
     * @param templateParams 模板参数，用于替换短信模板中的变量
     * @return 是否发送成功
     */
    boolean send(String mobile, SmsTypeEnum smsType, Map<String, String> templateParams);

}
