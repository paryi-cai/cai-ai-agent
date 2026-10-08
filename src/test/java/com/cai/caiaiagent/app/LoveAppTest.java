package com.cai.caiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
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

    /**
     * 元数据过滤检索：指定"已婚"状态，只从已婚篇文档中检索
     */
    @Test
    void testChatWithRagByStatus() {
        String chatId = UUID.randomUUID().toString();
        String message = "婚后总是因为家务分工吵架，怎么办？";
        String answer = loveApp.doChatWithRagByStatus(message, chatId, "已婚");
        Assertions.assertNotNull(answer);
    }

    /**
     * 空上下文兜底：问一个与恋爱无关的问题，云知识库检索不到 → 输出自定义友好提示
     */
    @Test
    void testChatWithRagCloudEmptyContext() {
        String chatId = UUID.randomUUID().toString();
        String message = "怎么用 Java 实现快速排序？";
        String answer = loveApp.doChatWithRagCloud(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("恋爱") || answer.contains("抱歉"),
                "空上下文时应返回友好兜底话术，实际返回：" + answer);
    }

    /**
     * QuestionAnswerAdvisor 动态过滤：运行时指定过滤表达式 status == '单身'
     */
    @Test
    void testChatWithRagByFilter() {
        String chatId = UUID.randomUUID().toString();
        String message = "怎么扩大社交圈？";
        String answer = loveApp.doChatWithRagByFilter(message, chatId, "status == '单身'");
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"文件操作工具"保存恋爱档案
     */
    @Test
    void testChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我保存我的恋爱档案为文件，文件名 love-profile.txt，内容：用户鱼皮，恋爱状态：恋爱中";
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"PDF 生成工具"
     */
    @Test
    void testChatWithToolsPDF() {
        String chatId = UUID.randomUUID().toString();
        String message = "直接调用工具生成一份《七夕约会计划》PDF，包含餐厅预订、活动流程和礼物清单";
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"联网搜索工具"获取实时信息
     * （需要配置 search-api.api-key）
     */
    @Test
    void testChatWithToolsWebSearch() {
        String chatId = UUID.randomUUID().toString();
        String message = "周末想带女朋友去上海约会，请联网搜索推荐几个适合情侣的小众打卡地？";
        String answer = loveApp.doChatWithTools(message, chatId);
        System.out.println("最终回答：" + answer);
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具上下文：工具从内部 ToolContext 读取"登录用户"信息（AI 看不到这些值）
     */
    @Test
    void testChatWithToolsContext() {
        String chatId = UUID.randomUUID().toString();
        String message = "请调用工具查询我自己（当前登录用户）的档案信息";
        // 模拟从登录态获取的内部上下文（不会发送给 AI 模型）
        Map<String, Object> toolContext = Map.of(
                "userName", "鱼皮",
                "loveStatus", "恋爱中");
        String answer = loveApp.doChatWithToolsByContext(message, chatId, toolContext);
        System.out.println("最终回答：" + answer);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("鱼皮"), "回答应包含从 ToolContext 读取的用户信息，实际：" + answer);
    }

    /**
     * 教程作业验证：让 AI 自主调用"时间工具"（第 7 个自研工具）
     */
    @Test
    void testChatWithToolsTime() {
        String chatId = UUID.randomUUID().toString();
        String message = "请调用时间工具告诉我：现在是几点？今天几号？";
        String answer = loveApp.doChatWithTools(message, chatId);
        System.out.println("最终回答：" + answer);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains(String.valueOf(java.time.LocalDate.now().getYear())),
                "回答应包含当前年份（说明用到了时间工具），实际：" + answer);
    }
}
