package com.cai.caiaiagent.controller;

import com.cai.caiaiagent.agent.CaiManus;
import com.cai.caiaiagent.app.FoodApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * AI 应用接口控制器：提供同步与 SSE 流式两种调用方式（菜狗探店 + 超级智能体）
 *
 * 完整路径 = /api（context-path 全局前缀）+ /ai（本类映射）
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private FoodApp foodApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用：一次性返回完整回答
     */
    @GetMapping("food_app/chat/sync")
    public String doChatWithFoodAppSync(String message, String chatId) {
        return foodApp.doChatWithTools(message, chatId);
    }

    /**
     * SSE 方式1（声明式）：直接返回 Flux<String>，由 Spring 自动桥接为 SSE 流
     * 可选参数 region：按区域过滤知识库检索（南山/福田/罗湖/宝安），不传则全库检索
     */
    @GetMapping(value = "food_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithFoodAppSSE(String message, String chatId, String region) {
        // region 为空 = 全库检索；指定区域 = 只检索该区域的知识库内容
        if (region != null && !region.isBlank()) {
            return foodApp.doChatByStreamByRegion(message, chatId, region);
        }
        return foodApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 方式2：返回 Flux<ServerSentEvent>，可自定义事件字段（event/id/retry）
     */
    @GetMapping(value = "food_app/chat/sse2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithFoodAppServerSentEvent(String message, String chatId) {
        return foodApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("chunk")
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 方式3（手动推送）：返回 SseEmitter，自己订阅数据流逐条推送给客户端
     */
    @GetMapping("food_app/chat/sse3")
    public SseEmitter doChatWithFoodAppSseEmitter(String message, String chatId) {
        // 创建 SSE 发射器（3 分钟超时）
        SseEmitter emitter = new SseEmitter(180000L);
        // 订阅 AI 流式输出：每收到一个片段就推送给客户端
        foodApp.doChatByStream(message, chatId)
                .subscribe(
                        // 每收到一个数据片段
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        // 出错时
                        emitter::completeWithError,
                        // 完成时
                        emitter::complete
                );
        // 返回 emitter（Spring 会挂起请求，持续向客户端推送）
        return emitter;
    }

    /**
     * 流式调用 CaiManus 超级智能体：每一步的执行结果会实时推送（分步"直播"）
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        // 智能体是有状态对象，每次请求必须创建新实例
        CaiManus caiManus = new CaiManus(allTools, dashscopeChatModel);
        return caiManus.runStream(message);
    }
}
