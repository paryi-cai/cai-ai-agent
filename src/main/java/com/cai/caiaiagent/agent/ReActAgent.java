package com.cai.caiaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct（Reasoning + Acting）模式的智能体抽象类
 *
 * 把 BaseAgent 的单步执行拆解为两个阶段：
 * 1. think：思考——分析当前状态，决定是否需要行动、行动什么
 * 2. act：行动——执行工具调用等具体动作
 *
 * 这是智能体最经典的执行范式：思考 → 行动 → 观察结果 → 再思考……
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    /**
     * 思考阶段：处理当前状态并决定下一步行动
     *
     * @return true=需要执行行动；false=无需行动（本轮只思考）
     */
    public abstract boolean think();

    /**
     * 行动阶段：执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单步 = 思考 + 行动（模板方法）
     *
     * 1. think()：决定是否需要行动
     * 2. 无需行动 → 返回"思考完成 - 无需行动"
     * 3. 需要行动 → 返回 act() 的执行结果
     * 4. try-catch 兜底：单步失败不影响整体循环，返回失败信息即可
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            // 单步失败不影响整体循环：记录并作为该步结果返回，模型下一轮可换策略
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
