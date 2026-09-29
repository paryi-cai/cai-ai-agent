package com.cai.caiaiagent.demo.invoke;

import cn.hutool.setting.yaml.YamlUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * API Key 统一读取工具
 * （接口里的字段默认就是 public static final 常量，所以能直接 TestApiKey.API_KEY 使用）
 *
 * 读取优先级：
 * 1. 环境变量 AI_DASHSCOPE_API_KEY（适合作业提交 / 服务器部署）
 * 2. 项目内的 application-local.yml（适合本地开发，文件已被 .gitignore 保护不会泄露）
 * 3. 兜底占位符（调用时会报鉴权错误，提醒你还没配置 key）
 *
 * 好处：密钥只填一处，4 种调用示例都能用，且绝不写死在代码里
 */
public interface TestApiKey {

    String API_KEY = loadApiKey();

    static String loadApiKey() {
        // 优先级 1：读环境变量
        String envKey = System.getenv("AI_DASHSCOPE_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        // 优先级 2：读 classpath 下的 application-local.yml（编译后资源会被复制到 target/classes）
        try (InputStream inputStream = TestApiKey.class.getResourceAsStream("/application-local.yml")) {
            if (inputStream != null) {
                // 用 Hutool 把 YAML 解析成 Map，再逐层取出 spring.ai.dashscope.api-key 的值
                Map<String, Object> yaml = YamlUtil.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                if (yaml.get("spring") instanceof Map<?, ?> spring && spring.get("ai") instanceof Map<?, ?> ai
                        && ai.get("dashscope") instanceof Map<?, ?> dashscope && dashscope.get("api-key") != null) {
                    String key = dashscope.get("api-key").toString().trim();
                    if (!key.isBlank()) {
                        return key;
                    }
                }
            }
        } catch (Exception ignored) {
            // 读取失败不抛异常，走下面的兜底值即可
        }
        // 优先级 3：兜底占位符
        return "sk-not-configured";
    }
}
