package com.cai.caiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

/**
 * LoveApp 单元测试
 * 注意：测试会真实调用大模型 API，会消耗少量 token 额度
 */
@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    /**
     * 多轮对话：验证对话记忆是否生效
     * 第三轮问"另一半叫什么"，如果 AI 能答出"编程导航"，说明它记住了前文
     */
    @Test
    void testChat() {
        // 每个测试用例用独立的 chatId，避免不同用例之间互相"串记忆"
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员鱼皮";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想让另一半（编程导航）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 结构化输出：验证 AI 输出能被自动转换为 LoveReport 对象
     */
    @Test
    void testChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    /**
     * PromptTemplate 模板渲染
     */
    @Test
    void testChatWithTemplate() {
        String answer = loveApp.doChatWithTemplate("鱼皮", "我想让另一半更爱我");
        Assertions.assertNotNull(answer);
    }

    /**
     * RAG 知识库问答：故意提问一个知识库文档里有答案的问题
     * （"婚后关系不太亲密" 已婚篇文档第 1 问有对应建议）
     */
    @Test
    void testChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 云知识库 RAG 问答（阿里云百炼平台）
     * 前置条件：百炼控制台已创建同名知识库并导入文档
     */
    @Test
    void testChatWithRagCloud() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer = loveApp.doChatWithRagCloud(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
