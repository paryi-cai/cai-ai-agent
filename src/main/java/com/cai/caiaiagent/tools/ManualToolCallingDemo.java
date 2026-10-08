package com.cai.caiaiagent.tools;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * 用户控制的工具执行演示（对比默认的"框架控制"模式）
 *
 * 背景：默认情况下 Spring AI 框架自动完成"模型请求工具 → 执行 → 结果回传 → 再请求"的整个循环。
 * 通过 withInternalToolExecutionEnabled(false) 关掉自动执行后，我们可以自己写循环，
 * 从而在工具执行前后插入自定义逻辑（进度追踪、日志、重试、条件判断等）。
 *
 * 核心组件：ToolCallingManager —— 负责"解析工具定义"和"执行模型请求的工具调用"
 */
@Slf4j
@Component
public class ManualToolCallingDemo {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallback[] allTools;

    /**
     * 手动控制工具调用循环的对话
     *
     * @param question    用户问题
     * @param toolContext 工具上下文（可选，内部参数不暴露给 AI）
     */
    public String chatWithManualToolExecution(String question, Map<String, Object> toolContext) {
        // 创建工具调用管理器（默认实现 DefaultToolCallingManager）
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        // 关键：禁用框架自动执行工具，改为由我们自己控制循环
        DashScopeChatOptions chatOptions = DashScopeChatOptions.builder()
                .withToolCallbacks(Arrays.asList(allTools))
                .withInternalToolExecutionEnabled(false)
                .withToolContext(toolContext)
                .build();

        Prompt prompt = new Prompt(question, chatOptions);
        ChatResponse chatResponse = dashscopeChatModel.call(prompt);

        int round = 0;
        // 手动处理工具调用循环：只要模型还在请求工具，就执行工具并把结果回传
        while (chatResponse.hasToolCalls()) {
            round++;
            log.info("【手动循环】第 {} 轮：模型请求调用工具，准备执行...", round);
            // 执行模型请求的所有工具调用，返回包含工具结果的新对话历史
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            log.info("【手动循环】工具执行完成，对话历史消息数：{}", toolExecutionResult.conversationHistory().size());
            // 用包含工具结果的历史构造新 Prompt，再次请求模型
            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
            chatResponse = dashscopeChatModel.call(prompt);
        }
        log.info("【手动循环】共执行 {} 轮工具调用，生成最终回答", round);
        return chatResponse.getResult().getOutput().getText();
    }
}
