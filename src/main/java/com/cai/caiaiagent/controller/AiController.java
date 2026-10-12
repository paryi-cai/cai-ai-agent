package com.cai.caiaiagent.controller;

import com.cai.caiaiagent.agent.YuManus;
import com.cai.caiaiagent.app.LoveApp;
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
 * AI 应用接口控制器：提供同步与 SSE 流式两种调用方式
 *
 * 完整路径 = /api（context-path 全局前缀）+ /ai（本类映射）
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用：一次性返回完整回答
     */
    @GetMapping("love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChatWithTools(message, chatId);
    }

    /**
     * SSE 方式1（声明式）：直接返回 Flux<String>，由 Spring 自动桥接为 SSE 流
     */
    @GetMapping(value = "love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 方式2：返回 Flux<ServerSentEvent>，可自定义事件字段（event/id/retry）
     */
    @GetMapping(value = "love_app/chat/sse2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("chunk")
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 方式3（手动推送）：返回 SseEmitter，自己订阅数据流逐条推送给客户端
     */
    @GetMapping("love_app/chat/sse3")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        // 创建 SSE 发射器（3 分钟超时）
        SseEmitter emitter = new SseEmitter(180000L);
        // 订阅 AI 流式输出：每收到一个片段就推送给客户端
        loveApp.doChatByStream(message, chatId)
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
     * 流式调用 manus
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, String chatId) {
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }
}
