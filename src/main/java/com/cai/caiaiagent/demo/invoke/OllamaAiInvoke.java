package com.cai.caiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
// 【已停用】Ollama 相关注解导入（不使用本地模型）
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.stereotype.Component;

/**
 * 扩展：用 Spring AI 调用"本地部署"的大模型（Ollama）——【已停用】
 *
 * 当前不使用本地模型，注册注解已注释，本类不会生效，仅作学习参考。
 * 如需重新启用（三步）：
 * 1. pom.xml：取消 spring-ai-starter-model-ollama 依赖的注释
 * 2. application.yml：取消 ollama 配置的注释
 * 3. 本类：取消下面两行注册注解的注释（并把 demo.ollama.enabled 配置改为 true）
 *
 * 前置条件：本地已安装并启动 Ollama（默认端口 11434），且已执行 ollama pull gemma3:1b
 */
// @Component
// @ConditionalOnProperty(name = "demo.ollama.enabled", havingValue = "true")
public class OllamaAiInvoke implements CommandLineRunner {

    /** Spring AI 自动配置的本地 Ollama 模型（字段名 ollamaChatModel 对应 Bean 名称） */
    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) throws Exception {
        // 调用方式和云端模型完全一致 —— 这正是 Spring AI 抽象层的价值：换模型、不改代码
        AssistantMessage output = ollamaChatModel.call(new Prompt("你好，我是鱼皮"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}
