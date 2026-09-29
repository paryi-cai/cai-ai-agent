package com.cai.caiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 方式 3：使用 Spring AI 调用大模型 —— 本项目后续的主力方式！
 *
 * 三个知识点：
 * 1. Spring AI 用统一的 ChatModel 接口屏蔽各家大模型差异，切换模型几乎不改代码
 * 2. @Component 注册为 Spring Bean；@ConditionalOnProperty 条件注册：
 *    仅当配置 demo.spring-ai.enabled=true 时才创建该 Bean
 *    （等价于教程里"注释/取消注释 @Component"的开关效果，但更优雅，不用改代码）
 * 3. 实现 CommandLineRunner：项目启动完成后自动执行一次 run 方法，适合写验证代码
 *
 * 运行方式：把 application-local.yml 中 demo.spring-ai.enabled 改为 true 后启动项目
 */
@Component
@ConditionalOnProperty(name = "demo.spring-ai.enabled", havingValue = "true")
public class SpringAiAiInvoke implements CommandLineRunner {

    /**
     * Spring AI Alibaba 自动配置好的通义千问模型
     * @Resource 按"字段名"注入，Bean 名称就是 dashscopeChatModel
     */
    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        // Prompt：Spring AI 统一的提示词对象（这里是最简单的纯文本提示）
        // getResult().getOutput()：取出模型回复对象（AssistantMessage），getText() 拿文本
        AssistantMessage output = dashscopeChatModel.call(new Prompt("你好，我是菜狗"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}
