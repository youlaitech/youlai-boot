package com.youlai.boot.config;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文档端点能力测试
 *
 * 注解包版本被依赖降级后，编译和其它单测都不会报错，只有打开文档页时才会失败
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
class OpenApiDocsTest {

    // 文档页需要这个方法，注解包版本偏低时这里会失败
    @Test
    void swaggerAnnotationsProvideDynamicRef() {
        assertThat(Schema.class.getMethods())
                .extracting(Method::getName)
                .contains("$dynamicRef");
    }

    // 两个包版本必须一致，混用会让文档页加载失败
    @Test
    void swaggerAnnotationsVersionMatchesCore() {
        String annotations = jarVersion(Schema.class);
        String core = jarVersion(io.swagger.v3.core.util.Json.class);
        assertThat(annotations).as("swagger-annotations 版本").isNotBlank();
        assertThat(core).as("swagger-core 版本").isNotBlank();
        assertThat(annotations).as("注解包与 core 版本").isEqualTo(core);
    }

    private static String jarVersion(Class<?> type) {
        URL location = type.getProtectionDomain().getCodeSource().getLocation();
        String fileName = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        Matcher matcher = Pattern.compile("-(\\d+\\.\\d+\\.\\d+)\\.jar$").matcher(fileName);
        return matcher.find() ? matcher.group(1) : "";
    }
}
