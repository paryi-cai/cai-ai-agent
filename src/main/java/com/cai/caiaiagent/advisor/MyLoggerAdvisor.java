package com.cai.caiaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/**
 * 自定义日志 Advisor（1.0 推荐的简化写法：实现 BaseAdvisor）
 *
 * BaseAdvisor 同时继承了 CallAdvisor（同步）和 StreamAdvisor（流式），
 * 并提供了默认的环绕逻辑：before（前置处理）→ 调用链 → after（后置处理），
 * 所以只需要实现 before / after / getOrder，不需要自己写链式调用代码。
 *
 * 官方内置的 MessageChatMemoryAdvisor、SimpleLoggerAdvisor 内部也是用 BaseAdvisor 实现的。
 * 对比：ReReadingAdvisor 用的是"完整版"写法（自己实现 CallAdvisor + StreamAdvisor），
 * 适合需要完全控制调用链的场景。
 */
@Slf4j
public class MyLoggerAdvisor implements BaseAdvisor {

    /** 前置处理：打印请求（这里只观察，不修改请求） */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        log.info("AI Request: {}", chatClientRequest.prompt());
        return chatClientRequest;
    }

    /** 后置处理：打印 AI 回复文本（这里只观察，不修改响应） */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
        return chatClientResponse;
    }

    /** 执行顺序：值越小越先执行 */
    @Override
    public int getOrder() {
        return 0;
    }
}
