package com.cai.caiaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cai.caiaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具调用智能体：ReAct 模式的具体落地
 *
 * think：把消息历史 + 工具清单发给大模型，拿到"是否调用工具、调用哪些工具"
 * act：用 ToolCallingManager 手动执行大模型请求的工具调用，并把结果写入消息上下文
 *
 * 关键点：必须禁用 Spring AI 的"托管工具调用"，让模型只"提出"请求、由我们手动执行
 * 版本提示：教程的 withProxyToolCalls(true) 在 1.0.0.2 已不存在，
 *          用 withInternalToolExecutionEnabled(false) 替代（第 6 章 ManualToolCallingDemo 已验证）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    /** 可用的工具 */
    private ToolCallback[] availableTools;

    /** 保存大模型响应（含工具调用请求），供 act 阶段执行 */
    private ChatResponse toolCallChatResponse;

    /** 工具调用管理器（负责解析并执行工具调用） */
    private ToolCallingManager toolCallingManager;

    /** 聊天选项：绑定工具 + 禁用框架托管执行（自己维护上下文和循环） */
    private DashScopeChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        // 工具调用管理器（默认实现 DefaultToolCallingManager）
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 聊天选项：绑定全部工具 + 禁用 Spring AI 托管执行（模型只返回"要调用哪些工具"的请求，由 act() 手动执行）
        this.chatOptions = DashScopeChatOptions.builder()
                .withToolCallbacks(Arrays.asList(availableTools))
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 思考阶段：请求大模型，获取需要调用的工具列表
     */
    @Override
    public boolean think() {
        // 每轮追加"下一步提示词"，引导模型持续自主行动
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            // 发起请求（由于禁用了托管执行，模型只返回工具调用请求，不会真正执行）
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();
            // 记录响应，供 act 阶段执行工具使用
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 输出思考内容与工具选择，便于观察智能体的决策过程
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info("{}的思考: {}", getName(), result);
            log.info("{}选择了 {} 个工具来使用", getName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                // 没有工具调用 = 模型直接给出回答：记录助手消息，本步无需行动
                getMessageList().add(assistantMessage);
                return false;
            }
            // 有工具调用：无需在此记录助手消息，执行工具时会自动补全上下文
            return true;
        } catch (Exception e) {
            log.error("{}的思考过程遇到了问题: {}", getName(), e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 行动阶段：执行工具调用并把结果写入消息上下文
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 手动执行工具调用；返回的 conversationHistory 已包含助手消息和工具返回结果
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 用执行后的完整历史更新消息上下文（注意：不要重复添加助手消息）
        setMessageList(toolExecutionResult.conversationHistory());
        // 提取本次工具调用结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 若调用了终止工具，则标记任务完成，让 Agent Loop 停止
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;
    }
}
