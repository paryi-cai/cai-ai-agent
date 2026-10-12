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
 * FoodApp 单元测试（菜狗探店）
 * 注意：测试会真实调用大模型 API，会消耗少量 token 额度
 */
@SpringBootTest
class FoodAppTest {

    @Resource
    private FoodApp foodApp;

    /** MCP 客户端自动注入的工具提供者（聚合所有 MCP 服务提供的工具） */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 多轮对话：验证对话记忆是否生效
     * 第三轮问"我最喜欢吃什么"，如果 AI 能答出"潮汕牛肉火锅"，说明它记住了前文
     */
    @Test
    void testChat() {
        // 每个测试用例用独立的 chatId，避免不同用例之间互相"串记忆"
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是菜狗，我最喜欢吃潮汕牛肉火锅";
        String answer = foodApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "帮我推荐一下深圳哪里能吃到好吃的牛肉火锅";
        answer = foodApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我刚才说我最喜欢吃什么来着？刚跟你说过，帮我回忆一下";
        answer = foodApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 结构化输出：验证 AI 输出能被自动转换为 FoodReport 对象
     */
    @Test
    void testChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是菜狗，周末想在深圳南山找一家适合拍照的餐厅，但我不知道选什么类型";
        FoodApp.FoodReport foodReport = foodApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(foodReport);
    }

    /**
     * PromptTemplate 模板渲染
     */
    @Test
    void testChatWithTemplate() {
        String answer = foodApp.doChatWithTemplate("菜狗", "想在深圳福田找一家适合约会的餐厅");
        Assertions.assertNotNull(answer);
    }

    /**
     * RAG 知识库问答：故意提问一个知识库文档里有答案的问题
     * （"南山适合约会拍照的餐厅" 南山篇 Q3 有对应建议）
     */
    @Test
    void testChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我在深圳南山，想找适合约会拍照的餐厅，有什么推荐？";
        String answer = foodApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 云知识库 RAG 问答（阿里云百炼平台）
     * 前置条件：百炼控制台已创建"菜狗探店问答"知识库并导入文档
     */
    @Test
    void testChatWithRagCloud() {
        String chatId = UUID.randomUUID().toString();
        String message = "深圳哪里能喝到正宗的广式早茶？";
        String answer = foodApp.doChatWithRagCloud(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 元数据过滤检索：指定"南山"区域，只从南山篇文档中检索
     */
    @Test
    void testChatWithRagByRegion() {
        String chatId = UUID.randomUUID().toString();
        String message = "科技园附近的工作日午餐有什么推荐？";
        String answer = foodApp.doChatWithRagByRegion(message, chatId, "南山");
        Assertions.assertNotNull(answer);
    }

    /**
     * 空上下文兜底：问一个与美食无关的问题，云知识库检索不到 → 输出自定义友好提示
     */
    @Test
    void testChatWithRagCloudEmptyContext() {
        String chatId = UUID.randomUUID().toString();
        String message = "怎么用 Java 实现快速排序？";
        String answer = foodApp.doChatWithRagCloud(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("探店") || answer.contains("美食") || answer.contains("抱歉"),
                "空上下文时应返回友好兜底话术（探店/美食/抱歉任一），实际返回：" + answer);
    }

    /**
     * QuestionAnswerAdvisor 动态过滤：运行时指定过滤表达式 region == '福田'
     */
    @Test
    void testChatWithRagByFilter() {
        String chatId = UUID.randomUUID().toString();
        String message = "福田哪里喝早茶好？";
        String answer = foodApp.doChatWithRagByFilter(message, chatId, "region == '福田'");
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"文件操作工具"保存探店笔记
     */
    @Test
    void testChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我保存我的探店笔记为文件，文件名 food-notes.txt，内容：博主菜狗，常驻深圳，最爱潮汕菜";
        String answer = foodApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"PDF 生成工具"
     */
    @Test
    void testChatWithToolsPDF() {
        String chatId = UUID.randomUUID().toString();
        String message = "直接调用工具生成一份《深圳一日美食探店计划》PDF，包含早茶、午餐、下午茶和晚餐安排";
        String answer = foodApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 工具调用：让 AI 自主调用"联网搜索工具"获取实时信息
     * （需要配置 search-api.api-key）
     */
    @Test
    void testChatWithToolsWebSearch() {
        String chatId = UUID.randomUUID().toString();
        String message = "请联网搜索推荐几个深圳适合探店打卡的宝藏小店？";
        String answer = foodApp.doChatWithTools(message, chatId);
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
                "userName", "菜狗",
                "tastePreference", "潮汕菜");
        String answer = foodApp.doChatWithToolsByContext(message, chatId, toolContext);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("菜狗"), "回答应包含从 ToolContext 读取的用户信息，实际：" + answer);
    }

    /**
     * 时间工具验证：让 AI 自主调用"时间工具"
     */
    @Test
    void testChatWithToolsTime() {
        String chatId = UUID.randomUUID().toString();
        String message = "请调用时间工具告诉我：现在是几点？今天几号？";
        String answer = foodApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains(String.valueOf(java.time.LocalDate.now().getYear())),
                "回答应包含当前年份（说明用到了时间工具），实际：" + answer);
    }

    /**
     * MCP 服务调用：AI 自主调用图片搜索 MCP 服务（本地 stdio 启动）
     * 前置条件：已打包 cai-image-search-mcp-server 的 jar
     */
    @Test
    void testChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些深圳美食的图片";
        String answer = foodApp.doChatWithMcp(message, chatId);
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
     * 高德地图 MCP：找火锅店场景
     * 前置条件：在 application-local.yml 配置有效的高德 API Key（或设置环境变量 AMAP_MAPS_API_KEY）
     * 注意：Key 无效（占位符）时工具会返回 INVALID_USER_KEY 错误并导致本测试失败；
     * 这也说明第三方 MCP 工具不做容错，使用前务必配置有效 Key
     */
    @Test
    void testChatWithAmapMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "我住在深圳南山区，请帮我找到 5 公里内评价好的潮汕牛肉火锅店";
        String answer = foodApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 流式主链路验证（RAG）：SSE 入口已挂载知识库，回答应引用知识库内容而非"凭记忆"
     */
    @Test
    void testChatByStreamWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "深圳的糖水铺有什么经典款？";
        String answer = String.join("", foodApp.doChatByStream(message, chatId).collectList().block());
        System.out.println("流式回答：" + answer);
        Assertions.assertFalse(answer.isBlank());
        Assertions.assertTrue(
                answer.contains("双皮奶") || answer.contains("杨枝甘露") || answer.contains("姜撞奶"),
                "流式链路应检索到知识库中的糖水内容（双皮奶/杨枝甘露/姜撞奶），实际：" + answer);
    }

    /**
     * 流式主链路验证（工具）：流式模式下工具调用应可用（用时间工具做低成本验证）
     */
    @Test
    void testChatByStreamWithTools() {
        String chatId = UUID.randomUUID().toString();
        String message = "请调用时间工具告诉我现在是哪一年、几月几号";
        String answer = String.join("", foodApp.doChatByStream(message, chatId).collectList().block());
        System.out.println("流式回答：" + answer);
        Assertions.assertTrue(answer.contains(String.valueOf(java.time.LocalDate.now().getYear())),
                "流式链路应能调用时间工具（回答含当前年份），实际：" + answer);
    }

    /**
     * 流式 + 区域过滤验证：指定"南山"区域，回答应引用南山篇知识（只检索该区域内容）
     */
    @Test
    void testChatByStreamByRegion() {
        String chatId = UUID.randomUUID().toString();
        String message = "科技园附近的工作日午餐有什么推荐？";
        String answer = String.join("", foodApp.doChatByStreamByRegion(message, chatId, "南山").collectList().block());
        System.out.println("流式回答（南山）：" + answer);
        Assertions.assertFalse(answer.isBlank());
        Assertions.assertTrue(
                answer.contains("科技园") || answer.contains("深大") || answer.contains("粤海")
                        || answer.contains("错峰") || answer.contains("美食广场"),
                "南山区域检索应命中南山篇内容，实际：" + answer);
    }
}
