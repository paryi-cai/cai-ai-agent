package com.cai.caiaiagent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 用户档案工具（演示"工具上下文 ToolContext"）
 *
 * ToolContext 的核心特点：
 * 1. 调用 AI 时通过 .toolContext(Map.of(...)) 在运行时传入
 * 2. 工具方法通过 ToolContext 参数读取这些值，但【不会发送给 AI 模型】
 * 3. 适合传递：登录用户信息、token、请求 ID 等敏感/内部参数
 *
 * 应用场景：用户说"帮我查询我的信息"，AI 不需要问"你是谁"——
 * 程序直接从登录态把 userId/userName 放进 ToolContext，工具内部使用即可
 */
public class UserProfileTool {

    @Tool(description = "Get the current logged-in user's own profile (name and love status)")
    public String getMyProfile(ToolContext toolContext) {
        // 从内部上下文读取（这些值不会暴露给 AI 模型）
        Object userName = toolContext.getContext().get("userName");
        Object loveStatus = toolContext.getContext().get("loveStatus");
        return "当前登录用户：" + userName + "，恋爱状态：" + loveStatus;
    }
}
