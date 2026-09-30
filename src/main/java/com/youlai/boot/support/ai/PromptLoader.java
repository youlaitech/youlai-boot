package com.youlai.boot.support.ai;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词加载器
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Slf4j
@Component
public class PromptLoader {

    // 提示词文件位置
    private static final String LOCATION_PATTERN = "classpath*:prompts/**/*.md";

    // 提示词目录，用于从资源地址截取提示词名称
    private static final String DIRECTORY = "prompts/";

    // 占位符左标记
    private static final String PLACEHOLDER_LEFT = "{{";

    // 占位符右标记
    private static final String PLACEHOLDER_RIGHT = "}}";

    // 提示词内容，键为 prompts 下的相对路径
    private final Map<String, String> prompts = new ConcurrentHashMap<>();

    /**
     * 载入全部提示词文件，读取失败则应用启动失败
     */
    @PostConstruct
    public void load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION_PATTERN);
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8).trim();
                    prompts.put(resolveName(resource), content);
                }
            }
            log.info("提示词载入完成, {} 个", prompts.size());
        } catch (IOException e) {
            throw new IllegalStateException("提示词载入失败", e);
        }
    }

    /**
     * 获取提示词原文
     *
     * @param name 提示词名称，prompts 下的相对路径，如 codegen/system.md
     * @return 提示词原文
     */
    public String get(String name) {
        String prompt = prompts.get(name);
        if (prompt == null) {
            throw new IllegalStateException("提示词不存在：" + name);
        }
        return prompt;
    }

    /**
     * 获取提示词并替换占位符，替换后仍残留 {{ 的视为占位符漏传，直接抛出异常
     *
     * @param name      提示词名称，prompts 下的相对路径
     * @param variables 变量值，键与文本中的 {{键}} 对应
     * @return 替换后的提示词
     */
    public String render(String name, Map<String, String> variables) {
        String prompt = get(name);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            prompt = prompt.replace(PLACEHOLDER_LEFT + variable.getKey() + PLACEHOLDER_RIGHT,
                    StrUtil.nullToEmpty(variable.getValue()));
        }
        int index = prompt.indexOf(PLACEHOLDER_LEFT);
        if (index >= 0) {
            int end = prompt.indexOf(PLACEHOLDER_RIGHT, index);
            String placeholder = end > index
                    ? prompt.substring(index, end + PLACEHOLDER_RIGHT.length())
                    : prompt.substring(index);
            throw new IllegalStateException("提示词 " + name + " 存在未替换的占位符：" + placeholder);
        }
        return prompt;
    }

    /**
     * 截取资源地址中 prompts 之后的路径作为提示词名称
     *
     * @param resource 提示词资源
     * @return 提示词名称
     */
    private String resolveName(Resource resource) throws IOException {
        String url = resource.getURL().toString();
        int index = url.lastIndexOf(DIRECTORY);
        if (index < 0) {
            throw new IllegalStateException("提示词路径异常：" + url);
        }
        return url.substring(index + DIRECTORY.length());
    }
}
