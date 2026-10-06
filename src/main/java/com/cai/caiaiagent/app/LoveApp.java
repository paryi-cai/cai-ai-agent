package com.cai.caiaiagent.app;

import com.cai.caiaiagent.advisor.MyLoggerAdvisor;
import com.cai.caiaiagent.chatmemory.FileBasedChatMemory;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 恋爱大师应用
 *
 * 核心设计（对应教程的方案设计部分）：
 * 1. 系统提示词：定义 AI 的"人设"——深耕恋爱心理的专家，会主动引导用户倾诉
 * 2. 多轮对话：通过 ChatClient + MessageChatMemoryAdvisor + ChatMemory 实现"记忆"
 */
@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    /**
     * RAG 知识库向量存储
     * 由 LoveAppVectorStoreConfig 创建：项目启动时自动加载 document/ 下的知识文档并向量化入库
     */
    @Resource
    private VectorStore loveAppVectorStore;

    /**
     * 云知识库检索增强 Advisor（阿里云百炼）
     * 由 LoveAppRagCloudAdvisorConfig 创建，按 application.yml 的 cloud-index-name 对接云端知识库
     */
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    /**
     * 系统提示词（System Prompt）：AI 应用的"灵魂"
     * 决定了 AI 的角色定位、行为模式和交互风格
     */
    private static final String SYSTEM_PROMPT = """
            扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。
            围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；
            恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。
            引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。""";

    public LoveApp(ChatModel dashscopeChatModel) {
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
                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())
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
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 恋爱报告（结构化输出的目标类型）
     * 使用 Java record 快速定义不可变数据类，等价于写了 getter/构造器/equals 的普通类
     */
    public record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 多轮对话 + 结构化输出
     * entity(LoveReport.class) 会自动：
     * 1. 调用前：把 LoveReport 转成 JSON Schema 格式指令附加到提示词，引导 AI 输出 JSON
     * 2. 调用后：把 AI 返回的 JSON 文本反序列化成 LoveReport 对象
     *
     * @return 恋爱报告对象（含标题和建议列表）
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为用户的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * PromptTemplate 模板特性演示
     * 把带占位符的模板渲染成最终提示词，好处：提示词结构化、可维护、可复用
     *
     * @param name     用户昵称（替换模板中的 {name}）
     * @param question 情感问题（替换模板中的 {question}）
     * @return AI 回复
     */
    public String doChatWithTemplate(String name, String question) {
        String templateText = """
                用户昵称：{name}
                情感问题：{question}
                请先共情用户，再给出 3 条可执行的建议，语气温暖。""";
        // 创建模板并渲染：{name}、{question} 会被替换为真实值
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        String renderedPrompt = promptTemplate.render(Map.of("name", name, "question", question));
        return chatClient.prompt().user(renderedPrompt).call().content();
    }
}
