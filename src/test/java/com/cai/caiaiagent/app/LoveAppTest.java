package com.cai.caiaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    /** MCP 客户端自动注入的工具提供者（聚合所有 MCP 服务提供的工具） */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

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
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains(String.valueOf(java.time.LocalDate.now().getYear())),
                "回答应包含当前年份（说明用到了时间工具），实际：" + answer);
    }

    /**
     * MCP 服务调用：AI 自主调用图片搜索 MCP 服务（本地 stdio 启动）
     * 前置条件：已打包 yu-image-search-mcp-server 的 jar
     */
    @Test
    void testChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些哄另一半开心的图片";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 查看当前加载的所有 MCP 工具（验证图片搜索 + 高德地图 MCP 均就绪）
     * 仅验证工具发现，不调用 AI、不消耗 token、不需要 API Key
     */
    @Test
    void testListMcpTools() {
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        StringBuilder sb = new StringBuilder("\n===== MCP 工具清单（共 " + toolCallbacks.length + " 个）=====\n");
        for (ToolCallback toolCallback : toolCallbacks) {
            sb.append("- ").append(toolCallback.getToolDefinition().name()).append("\n");
        }
        System.out.println(sb);
        // 图片搜索 MCP 服务的工具（MCP 工具名会被 Spring AI 加前缀：spring_ai_mcp_client_{连接名}_{工具名}）
        boolean hasImageSearch = java.util.Arrays.stream(toolCallbacks)
                .anyMatch(tc -> tc.getToolDefinition().name().contains("searchImage"));
        Assertions.assertTrue(hasImageSearch, "应加载图片搜索 MCP 服务的 searchImage 工具");
        // 高德地图 MCP 服务的工具（原工具名以 maps_ 开头）
        boolean hasAmap = java.util.Arrays.stream(toolCallbacks)
                .anyMatch(tc -> tc.getToolDefinition().name().contains("maps_"));
        Assertions.assertTrue(hasAmap, "应加载高德地图 MCP 服务的工具（maps_ 前缀）");
    }

    /**
     * 高德地图 MCP：第 7 章原始需求场景（根据位置推荐约会地点）
     * 前置条件：在 application-local.yml 配置有效的高德 API Key（或设置环境变量 AMAP_MAPS_API_KEY）
     * 注意：Key 无效（占位符）时工具会返回 INVALID_USER_KEY 错误并导致本测试失败；
     * 这也说明第三方 MCP 工具不做容错，使用前务必配置有效 Key
     */
    @Test
    void testChatWithAmapMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
