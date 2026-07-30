package com.youlai.boot.support.sms.impl;

import cn.hutool.json.JSONUtil;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.youlai.boot.support.sms.SmsTypeEnum;
import com.youlai.boot.config.property.AliyunSmsProperties;
import com.youlai.boot.support.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 阿里云短信服务实现
 *
 * @author Ray
 * @since 3.1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "sms.type", havingValue = "aliyun", matchIfMissing = true)
@RequiredArgsConstructor
public class AliyunSmsServiceImpl implements SmsService {

    private final AliyunSmsProperties aliyunSmsProperties;

    @Override
    public boolean send(String mobile, SmsTypeEnum smsType, Map<String, String> templateParams) {
        // 根据 smsType 从配置中获取对应的阿里云短信模板编码
        String templateCode = aliyunSmsProperties.getTemplates().get(smsType.getValue());

        DefaultProfile profile = DefaultProfile.getProfile(aliyunSmsProperties.getRegionId(),
                aliyunSmsProperties.getAccessKeyId(), aliyunSmsProperties.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(aliyunSmsProperties.getDomain());
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", aliyunSmsProperties.getRegionId());
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", aliyunSmsProperties.getSignName());
        request.putQueryParameter("TemplateCode", templateCode);
        request.putQueryParameter("TemplateParam", JSONUtil.toJsonStr(templateParams));

        try {
            CommonResponse response = client.getCommonResponse(request);
            return response.getHttpResponse().isSuccess();
        } catch (ClientException e) {
            log.error("发送短信失败: {}", e.getMessage());
        }
        return false;
    }
}
