package com.cai.caiaiagent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 自定义 Re-Reading（Re2）Advisor —— 提高大模型的推理能力
 *
 * 原理（论文 https://arxiv.org/pdf/2309.06275）：
 * 把用户提示词改写成"重复阅读"格式，让模型带着问题再读一遍，从而提升推理准确率：
 *   {原始问题}
 *   Read the question again: {原始问题}
 *
 * 注意：该技术会让输入 Token 翻倍（成本加倍），面向 C 端的应用慎用
 *
 * 升级说明（对照教程第 10 章）：
 * 1.0 版本中 Advisor 接口从 CallAroundAdvisor/StreamAroundAdvisor 变为 CallAdvisor/StreamAdvisor，
 * 请求对象从 AdvisedRequest 变为 ChatClientRequest（Prompt + context 只读记录类），
 * 修改提示词要用 Prompt.augmentUserMessage() 而不是 setter
 */
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    /** 前置处理：改写用户提示词 */
    private ChatClientRequest before(ChatClientRequest chatClientRequest) {
        // 取出原始用户输入
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        // 按 Re2 格式拼接：原文 + 再读一遍
        String augmentedUserText = """
                %s
                Read the question again: %s
                """.formatted(userText, userText);
        // 基于原 Prompt 生成"改写后"的新 Prompt（record 不可变，所以要创建新对象）
        Prompt augmentedPrompt = chatClientRequest.prompt().augmentUserMessage(augmentedUserText);
        return new ChatClientRequest(augmentedPrompt, chatClientRequest.context());
    }

    /** 非流式：改写后继续传给下一个 Advisor */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        return chain.nextCall(this.before(chatClientRequest));
    }

    /** 流式：改写后继续传给下一个 Advisor */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        return chain.nextStream(this.before(chatClientRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
