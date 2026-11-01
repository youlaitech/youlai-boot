package com.youlai.boot.support.ai;

import com.sun.net.httpserver.HttpServer;
import com.youlai.boot.common.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用真实配置与本地 HTTP 桩验证 AI 装配及兼容协议，不连接数据库或外部模型服务
 */
class AiConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    ToolCallingAutoConfiguration.class,
                    ChatClientAutoConfiguration.class,
                    OpenAiChatAutoConfiguration.class,
                    OpenAiEmbeddingAutoConfiguration.class,
                    OpenAiImageAutoConfiguration.class,
                    OpenAiModerationAutoConfiguration.class,
                    OpenAiAudioSpeechAutoConfiguration.class,
                    OpenAiAudioTranscriptionAutoConfiguration.class))
            .withBean(AiChatService.class);

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", "ai-default-test"})
    void disabledAiStartsWithoutCredentials(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile, "spring.ai.openai.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(ChatModel.class);
                    assertThatThrownBy(() -> context.getBean(AiChatService.class).chat("system", "test"))
                            .isInstanceOf(BusinessException.class)
                            .hasMessageContaining("spring.ai.model.chat");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void enabledAiBindsCurrentPropertiesAndPreservesVendorParameter(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile,
                        "spring.ai.model.chat=openai", "spring.ai.openai.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(ChatModel.class);
                    var options = context.getBean(OpenAiChatModel.class).getOptions();
                    assertThat(options.getModel()).isEqualTo(context.getEnvironment()
                            .getRequiredProperty("spring.ai.openai.chat.model"));
                    assertThat(options.getTemperature()).isEqualTo(0.3);
                    assertThat(options.getExtraBody()).containsEntry("enable_thinking", false)
                            .doesNotContainKey("enablethinking");
                    assertThat(context.getBean(OpenAiCommonProperties.class).getBaseUrl())
                            .isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void requestUsesOneVersionPrefixAndBooleanThinkingFlag(String profile) throws Exception {
        var requestPath = new AtomicReference<String>();
        var requestBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"id":"test-completion","object":"chat.completion","created":1,"model":"test-model",
                     "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
                     "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            runner.withPropertyValues("spring.profiles.active=" + profile,
                            "spring.ai.model.chat=openai", "spring.ai.openai.api-key=test-key",
                            "spring.ai.openai.base-url=http://127.0.0.1:" + server.getAddress().getPort()
                                    + "/compatible-mode/v1")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(AiChatService.class).chat("system", "test")).isEqualTo("ok");
                        assertThat(requestPath.get()).isEqualTo("/compatible-mode/v1/chat/completions");
                        var body = JsonMapper.builder().build().readTree(requestBody.get());
                        assertThat(body.has("enable_thinking")).isTrue();
                        assertThat(body.get("enable_thinking").isBoolean()).isTrue();
                        assertThat(body.get("enable_thinking").booleanValue()).isFalse();
                        assertThat(body.has("enablethinking")).isFalse();
                        assertThat(body.get("model").asText()).isEqualTo(context.getEnvironment()
                                .getRequiredProperty("spring.ai.openai.chat.model"));
                    });
        } finally {
            server.stop(0);
        }
    }
}
