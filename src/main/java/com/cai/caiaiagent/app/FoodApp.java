package com.cai.caiaiagent.app;

import com.cai.caiaiagent.advisor.MyLoggerAdvisor;
import com.cai.caiaiagent.chatmemory.FileBasedChatMemory;
import com.cai.caiaiagent.rag.FoodAppRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 菜狗探店 —— AI 美食探店博主应用（FoodApp）
 *
 * 核心设计：
 * 1. 系统提示词：定义 AI 的"人设"——深耕深圳本地的美食探店博主，会主动引导用户说明场景和口味
 * 2. 多轮对话：通过 ChatClient + MessageChatMemoryAdvisor + ChatMemory 实现"记忆"
 */
@Component
@Slf4j
public class FoodApp {

    private final ChatClient chatClient;

    /**
     * RAG 知识库向量存储
     * 由 FoodAppVectorStoreConfig 创建：项目启动时自动加载 document/ 下的深圳美食知识文档并向量化入库
     */
    @Resource
    private VectorStore foodAppVectorStore;

    /**
     * 云知识库检索增强 Advisor（阿里云百炼）
     * 由 FoodAppRagCloudAdvisorConfig 创建，按 application.yml 的 cloud-index-name 对接云端知识库
     */
    @Resource
    private Advisor foodAppRagCloudAdvisor;

    /**
     * 所有工具（由 ToolRegistration 统一注册）
     * AI 会根据用户需求自主决定是否调用、调用哪个工具
     */
    @Resource
    private ToolCallback[] allTools;

    /**
     * MCP 客户端提供的工具（来自配置的 MCP 服务，如本地图片搜索、高德地图）
     * 由 Spring AI MCP 客户端自动配置注入
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 系统提示词（System Prompt）：AI 应用的"灵魂"
     * 决定了 AI 的角色定位、行为模式和交互风格
     */
    private static final String SYSTEM_PROMPT = """
            你是"菜狗"，一位深耕深圳本地的美食探店博主。
            开场向用户表明身份，告知用户可以咨询深圳吃喝玩乐和探店相关的问题。
            围绕深圳各区（南山、福田、罗湖、宝安等）推荐餐厅、菜系和觅食路线：
            用户找店时，结合区域、菜系、预算给出具体建议；
            用户问菜品时，讲清楚特色、吃法和避雷点；
            引导用户说明用餐场景（约会、聚餐、一人食）和口味偏好，以便给出专属探店建议。""";

    public FoodApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件持久化的对话记忆：对话会保存到项目根目录的 chat-memory/ 下，服务重启不丢失
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 对比：基于内存 + 滑动窗口的写法（开发调试更方便，但重启即"失忆"）——
        // ChatMemory chatMemory = MessageWindowChatMemory.builder()
        //         .chatMemoryRepository(new InMemoryChatMemoryRepository())
        //         .maxMessages(20)
        //         .build();

        // 构造 ChatClient（链式调用风格的 AI 客户端，比直接使用 ChatModel 功能更丰富）
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 对话记忆 Advisor：调用 AI 前自动把历史对话拼进提示词，调用后自动保存本轮消息
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor：打印请求和响应，便于调试观察
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * 多轮对话（带对话记忆）
     *
     * @param message 用户输入
     * @param chatId  会话 id（相当于"房间号"，同一个 id 的对话共享记忆）
     * @return AI 回复文本
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                // 给 Advisor 动态指定会话 id：告诉对话记忆 Advisor"这轮对话存到哪个房间"
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + RAG 知识库问答
     * QuestionAnswerAdvisor 的工作流程：
     * 1. 调用前：把用户问题向量化，去向量库检索最相似的文档切片，拼进提示词（查询增强）
     * 2. 调用后：返回 AI 回答（检索到的文档也会放进 Advisor 上下文，可用于溯源）
     *
     * @param message 用户问题
     * @param chatId  会话 id（带记忆，多轮对话可用）
     * @return AI 基于知识库的回答
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 应用知识库问答（1.0 写法：QuestionAnswerAdvisor.builder(...)）
                .advisors(QuestionAnswerAdvisor.builder(foodAppVectorStore).build())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + 云知识库问答（阿里云百炼）
     * 与本地 RAG 的区别：检索走云端知识库服务（文档切分/存储/检索都在百炼平台完成）
     */
    public String doChatWithRagCloud(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 应用云知识库检索增强 Advisor
                .advisors(foodAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + 按区域过滤的知识库问答
     * 演示"元数据过滤检索"：只从指定区域（南山/福田/罗湖/宝安）的文档里检索，
     * 避免召回其他区域的无关内容，提升检索精度
     *
     * @param region 区域：南山 / 福田 / 罗湖 / 宝安
     */
    public String doChatWithRagByRegion(String message, String chatId, String region) {
        // 动态创建"带区域过滤"的 RAG Advisor（工厂模式）
        Advisor ragCustomAdvisor = FoodAppRagCustomAdvisorFactory.createFoodAppRagCustomAdvisor(foodAppVectorStore, region);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(ragCustomAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + 知识库问答（运行时动态指定元数据过滤条件）
     * 演示 QuestionAnswerAdvisor 的 FILTER_EXPRESSION 动态参数：
     * 与 doChatWithRagByRegion 的区别是"过滤表达式在运行时传入"，适合动态筛选场景
     *
     * @param filterExpression 过滤表达式（语法类似 SQL，如 "region == '南山'"）
     */
    public String doChatWithRagByFilter(String message, String chatId, String filterExpression) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        // 动态过滤表达式：作用在文档元数据上
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .advisors(QuestionAnswerAdvisor.builder(foodAppVectorStore).build())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + 工具调用
     * 把注册的所有工具交给 AI，让它自主决定何时调用（如搜索/抓取/下载/生成 PDF）
     *
     * @param message 用户需求
     * @param chatId  会话 id（保留对话记忆）
     * @return AI 的最终回答
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 绑定工具（1.0 写法：toolCallbacks 接收 ToolCallback 数组）
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + 工具调用 + 工具上下文（ToolContext）
     * 演示给工具传递"不暴露给 AI"的内部参数（如登录用户信息、token、请求 ID）
     *
     * @param toolContext 内部上下文参数（不会发送给 AI 模型，只在程序内部使用）
     */
    public String doChatWithToolsByContext(String message, String chatId, Map<String, Object> toolContext) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                // 工具上下文：随请求传入，工具内可读取，AI 不可见
                .toolContext(toolContext)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 多轮对话 + MCP 服务调用
     *
     * MCP 调用的本质仍然是工具调用：把 MCP 服务提供的工具交给 AI，
     * 由 AI 决定何时调用，客户端程序负责实际执行（stdio 模式下会拉起本地子进程运行服务）
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 绑定 MCP 服务提供的所有工具（1.0 写法：toolCallbacks 接收 ToolCallbackProvider）
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 探店报告（结构化输出的目标类型）
     * 使用 Java record 快速定义不可变数据类，等价于写了 getter/构造器/equals 的普通类
     */
    public record FoodReport(String title, List<String> suggestions) {
    }

    /**
     * 多轮对话 + 结构化输出
     * entity(FoodReport.class) 会自动：
     * 1. 调用前：把 FoodReport 转成 JSON Schema 格式指令附加到提示词，引导 AI 输出 JSON
     * 2. 调用后：把 AI 返回的 JSON 文本反序列化成 FoodReport 对象
     *
     * @return 探店报告对象（含标题和建议列表）
     */
    public FoodReport doChatWithReport(String message, String chatId) {
        FoodReport foodReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成探店结果，标题为用户的探店报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(FoodReport.class);
        log.info("foodReport: {}", foodReport);
        return foodReport;
    }

    /**
     * PromptTemplate 模板特性演示
     * 把带占位符的模板渲染成最终提示词，好处：提示词结构化、可维护、可复用
     *
     * @param name     用户昵称（替换模板中的 {name}）
     * @param question 探店需求（替换模板中的 {question}）
     * @return AI 回复
     */
    public String doChatWithTemplate(String name, String question) {
        String templateText = """
                用户昵称：{name}
                探店需求：{question}
                请先回应用户的需求，再给出 3 条可执行的探店建议，语气活泼专业。""";
        // 创建模板并渲染：{name}、{question} 会被替换为真实值
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        String renderedPrompt = promptTemplate.render(Map.of("name", name, "question", question));
        return chatClient.prompt().user(renderedPrompt).call().content();
    }

    /**
     * 流式对话（打字机效果），供 SSE 主入口使用（全库检索）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return doChatByStream(message, chatId, QuestionAnswerAdvisor.builder(foodAppVectorStore).build());
    }

    /**
     * 流式对话（按区域过滤检索）：只检索指定区域的知识库内容（南山/福田/罗湖/宝安）
     * 元数据过滤先于向量检索执行，避免召回其他区域的无关内容
     */
    public Flux<String> doChatByStreamByRegion(String message, String chatId, String region) {
        return doChatByStream(message, chatId,
                FoodAppRagCustomAdvisorFactory.createFoodAppRagCustomAdvisor(foodAppVectorStore, region));
    }

    /**
     * 流式对话内部实现：对话记忆 + RAG 检索 Advisor（全库或按区域）+ 全量工具（本地 + MCP）
     *
     * 说明：工具在流式模式下同样可用；RAG 检索避免模型"凭训练记忆"编造店名地址
     */
    private Flux<String> doChatByStream(String message, String chatId, Advisor ragAdvisor) {
        // 合并本地工具与 MCP 工具（高德地图/图片搜索），一次性绑定
        ToolCallback[] mergedTools = Stream.concat(
                        Arrays.stream(allTools),
                        Arrays.stream(toolCallbackProvider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);
        return chatClient
                .prompt()
                .user(message)
                // 对话记忆
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // RAG 知识库检索（全库或按区域过滤）
                .advisors(ragAdvisor)
                // 工具调用：AI 自主决定是否调用（本地工具 + MCP 工具）
                .toolCallbacks(mergedTools)
                .stream()
                .content();
    }
}
