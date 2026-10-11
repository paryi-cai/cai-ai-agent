package com.cai.caiaiagent.agent.model;

/**
 * 智能体执行状态枚举
 *
 * 通过状态机控制智能体的执行流程：空闲 → 运行中 → 已完成/出错
 * （BaseAgent 的执行循环靠它来判断是否继续）
 */
public enum AgentState {

    /** 空闲状态（可接收新任务） */
    IDLE,

    /** 运行中状态 */
    RUNNING,

    /** 已完成状态（任务结束，循环停止） */
    FINISHED,

    /** 错误状态 */
    ERROR
}
