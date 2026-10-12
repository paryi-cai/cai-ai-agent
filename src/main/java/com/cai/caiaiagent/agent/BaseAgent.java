package com.cai.caiaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.cai.caiaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     * 运行智能体（流式输出版）：与 run() 逻辑一致，区别是每一步的结果通过 SseEmitter 实时推送
     *
     * 工作原理：Web 线程立刻返回 emitter（不阻塞），后台线程执行 Agent Loop，
     * 每完成一步就 send 一次，客户端像"看直播"一样实时看到智能体的执行过程
     *
     * @param userPrompt 用户任务提示词
     * @return SseEmitter（Spring 会把推送内容按 SSE 协议发给客户端）
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建 SSE 发射器，设置较长的超时时间（智能体任务执行时间长）
        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时

        // 使用线程异步处理，避免长时间占用 Web 服务器线程池资源
        CompletableFuture.runAsync(() -> {
            try {
                // 状态校验（推送错误信息后直接结束）
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                // 切换到运行状态，记录用户任务
                state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                try {
                    // Agent Loop：每一步执行完立即推送结果
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);
                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;
                        // 实时推送这一步的结果
                        emitter.send(result);
                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 超时回调：连接超时后收敛状态，避免智能体卡在 RUNNING
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        // 完成回调：连接结束时确保资源清理、状态收敛
        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
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
