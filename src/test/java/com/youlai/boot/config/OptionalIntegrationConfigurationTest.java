package com.youlai.boot.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.auth.wxma.config.WxMaConfig;
import com.youlai.boot.auth.wxma.config.WxMaProperties;
import com.youlai.boot.support.mail.MailConfig;
import com.youlai.boot.support.mail.MailProperties;
import com.youlai.boot.support.sms.AliyunSmsProperties;
import com.youlai.boot.support.sms.service.impl.AliyunSmsServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 读取真实环境配置，验证空凭据和原生环境变量绑定；不发送请求或启动任务执行器。
 */
class OptionalIntegrationConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void optionalClientsStartWithoutCredentials(String profile) {
        profileRunner(profile, Map.of())
                .withUserConfiguration(WxMaProperties.class, WxMaConfig.class,
                        MailProperties.class, MailConfig.class, AliyunSmsProperties.class,
                        AliyunSmsServiceImpl.class, OidcProperties.class, XxlJobConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(WxMaService.class)
                            .hasSingleBean(JavaMailSenderImpl.class)
                            .hasSingleBean(AliyunSmsServiceImpl.class)
                            .doesNotHaveBean(OidcProperties.class)
                            .doesNotHaveBean(XxlJobConfig.class)
                            .doesNotHaveBean(XxlJobSpringExecutor.class);

                    var wx = context.getBean(WxMaProperties.class);
                    assertThat(wx.getProvider()).isEqualTo("wechat-mini-main");
                    assertThat(wx.getAppid()).isEmpty();
                    assertThat(wx.getSecret()).isEmpty();
                    assertThat(context.getBean(WxMaService.class).getWxMaConfig().getAppid()).isEmpty();

                    var mail = context.getBean(MailProperties.class);
                    assertThat(mail.getHost()).isEmpty();
                    assertThat(mail.getUsername()).isEmpty();
                    assertThat(mail.getPassword()).isEmpty();
                    assertThat(mail.getFrom()).isEmpty();
                    var sender = context.getBean(JavaMailSenderImpl.class);
                    assertThat(sender.getPort()).isEqualTo(587);
                    assertThat(sender.getJavaMailProperties())
                            .containsEntry("mail.smtp.auth", true)
                            .containsEntry("mail.smtp.starttls.enable", true);

                    var sms = context.getBean(AliyunSmsProperties.class);
                    assertThat(sms.getAccessKeyId()).isEmpty();
                    assertThat(sms.getAccessKeySecret()).isEmpty();
                    assertThat(sms.getSignName()).isEmpty();
                    assertThat(sms.getDomain()).isEqualTo("dysmsapi.aliyuncs.com");
                    assertThat(sms.getRegionId()).isEqualTo("cn-shanghai");
                    assertThat(sms.getTemplates()).containsOnlyKeys("register", "login", "change-mobile");
                    assertThat(sms.getTemplates().values()).containsOnly("");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void nativeEnvironmentVariablesOverridePlainYamlValues(String profile) {
        profileRunner(profile, Map.of(
                "SECURITY_OIDC_ENABLED", "true",
                "SECURITY_OIDC_ISSUER", "https://identity.example.test/realms/demo",
                "SECURITY_OIDC_CLIENTID", "test-client",
                "SECURITY_OIDC_CLIENTSECRET", "test-secret",
                "SECURITY_OIDC_FRONTENDCALLBACKURI", "http://localhost:3000/#/oidc/callback",
                "WX_MINIAPP_PROVIDER", "wechat-mini-test"))
                // 仅绑定属性，避免加载 OIDC 客户端并读取远端发现文档。
                .withUserConfiguration(OidcProperties.class, WxMaProperties.class)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(OidcProperties.class);
                    var oidc = context.getBean(OidcProperties.class);
                    assertThat(oidc.isEnabled()).isTrue();
                    assertThat(oidc.getIssuer()).isEqualTo("https://identity.example.test/realms/demo");
                    assertThat(oidc.getClientId()).isEqualTo("test-client");
                    assertThat(oidc.getClientSecret()).isEqualTo("test-secret");
                    assertThat(oidc.getFrontendCallbackUri()).isEqualTo("http://localhost:3000/#/oidc/callback");
                    assertThat(context.getBean(WxMaProperties.class).getProvider()).isEqualTo("wechat-mini-test");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void enablingOidcWithoutCredentialsFailsValidation(String profile) {
        profileRunner(profile, Map.of())
                .withUserConfiguration(OidcProperties.class)
                .withPropertyValues("security.oidc.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    private ApplicationContextRunner profileRunner(String profile, Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=" + profile)
                .withInitializer(context -> {
                    // 使用真正的系统环境属性源，同时隔离运行测试的机器所配置的第三方凭据。
                    context.getEnvironment().getPropertySources().replace(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new SystemEnvironmentPropertySource(
                                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment));
                    new ConfigDataApplicationContextInitializer().initialize(context);
                });
    }
}
