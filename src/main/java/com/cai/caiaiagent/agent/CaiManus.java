package com.cai.caiaiagent.agent;

import com.cai.caiaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * CaiManus：基于 Spring AI 实现的超级智能体（Manus 的简化版）
 *
 * 继承 ToolCallAgent 后，这里主要做"配置组装"：
 * 提示词、最大步数、ChatClient——逻辑一行不用写
 */
@Component
public class CaiManus extends ToolCallAgent {

    public CaiManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("caiManus");
        // 系统提示词：定义角色定位
        String SYSTEM_PROMPT = """
                You are CaiManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        // 下一步提示词：每轮循环追加，引导自主规划和逐步执行
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        // 最大步数：防止无限循环
        this.setMaxSteps(20);
        // 构建 ChatClient（挂上日志 Advisor，便于观察每一步的请求）
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
