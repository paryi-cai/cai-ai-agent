package com.cai.caiaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * 方式 4：使用 LangChain4j 调用大模型
 *
 * 和 Spring AI 定位类似（都是 Java AI 开发框架），但 API 设计和生态不同
 * 本项目以 Spring AI 为主，这里了解即可
 * 运行方式：直接右键运行 main 方法
 */
public class LangChainAiInvoke {

    public static void main(String[] args) {
        // 构建千问模型客户端（Builder 模式）
        ChatLanguageModel qwenModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        // chat()：一行完成对话并返回文本结果（对比 Spring AI 的 ChatModel.call() 链式调用风格）
        String answer = qwenModel.chat("我是程序员鱼皮，这是编程导航 codefather.cn 的原创项目教程");
        System.out.println(answer);
    }
}
