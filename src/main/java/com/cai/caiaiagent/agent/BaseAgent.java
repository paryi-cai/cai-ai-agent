package com.cai.caiaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.cai.caiaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础智能体类：管理智能体状态和多步骤执行流程（模板方法模式）
 *
 * 提供状态转换、消息上下文管理和"基于步骤的执行循环"的基础功能
 *
 * 设计要点：
 * 1. chatClient 由调用方传入（不写死具体模型，更灵活）
 * 2. messageList 自主维护消息上下文（不依赖框架的自动记忆机制）
 * 3. state 状态机控制执行流程（IDLE → RUNNING → FINISHED/ERROR）
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // ===== 核心属性 =====

    /** 智能体名称 */
    private String name;

    /** 系统提示词（定义智能体的角色和行为规范） */
    private String systemPrompt;

    /** 下一步提示词（每轮循环追加，引导智能体持续自主行动） */
    private String nextStepPrompt;

    // ===== 状态 =====

    /** 当前状态 */
    private AgentState state = AgentState.IDLE;

    // ===== 执行控制 =====

    /** 最大执行步数（防止无限循环） */
    private int maxSteps = 10;

    /** 当前执行到第几步 */
    private int currentStep = 0;

    // ===== LLM =====

    /** 大模型客户端 */
    private ChatClient chatClient;

    /** 消息上下文（自主维护的会话历史） */
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行智能体：执行"思考-行动"循环，直到任务完成或达到最大步数（模板方法）
     *
     * 执行流程：
     * 1. 状态校验（非 IDLE 状态或空提示词 → 抛异常）
     * 2. 置 RUNNING，把 userPrompt 加入 messageList
     * 3. 循环调用 step()，直到完成或达到 maxSteps，收集每步结果
     * 4. 兜底：异常置 ERROR 并返回错误信息，finally 里调用 cleanup() 清理资源
     *
     * @param userPrompt 用户任务提示词
     * @return 各步骤执行结果汇总
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Agent is not IDLE");
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("User prompt is blank");
        }

        this.state = AgentState.RUNNING;

        messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Step {}:", stepNumber);
                String stepResult = step();
                String result = "step " + stepNumber + ": " + stepResult;
                results.add(result);
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Reached max steps");
            }

            return String.join("\n", results);
        } catch (Exception e) {
            // 任何一步抛异常：标记错误状态并返回错误信息（不拖垮调用方）
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误: " + e.getMessage();
        } finally {
            // 无论成功（完成/超步数）还是失败，都要清理资源（子类可重写 cleanup）
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤（子类实现）—— 模板方法中的"抽象步骤"
     */
    public abstract String step();

    /**
     * 清理资源（钩子方法，子类按需重写，默认无操作）
     */
    protected void cleanup() {
    }
}
