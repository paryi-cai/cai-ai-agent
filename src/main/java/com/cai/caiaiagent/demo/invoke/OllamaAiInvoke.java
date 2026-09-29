package com.cai.caiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 扩展：用 Spring AI 调用"本地部署"的大模型（Ollama）
 *
 * 前置条件：
 * 1. 本地已安装并启动 Ollama（默认端口 11434）
 * 2. 已执行 ollama pull gemma3:1b 拉取模型（模型名在 application.yml 中配置）
 * 3. 把 demo.ollama.enabled 改为 true 开启本示例
 *
 * 对比云端大模型：数据不出本地、免费、可离线；但对硬件有要求、模型能力相对弱
 */
@Component
@ConditionalOnProperty(name = "demo.ollama.enabled", havingValue = "true")
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
