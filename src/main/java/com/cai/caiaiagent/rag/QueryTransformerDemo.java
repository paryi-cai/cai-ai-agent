package com.cai.caiaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询转换器演示：查询翻译 + 查询压缩（RAG 预检索阶段）
 *
 * 1. 查询翻译（TranslationQueryTransformer）：把查询翻译成目标语言，
 *    适合"embedding 模型只支持特定语言"的场景（教程提醒：成本比专业翻译 API 高，慎用）
 * 2. 查询压缩（CompressionQueryTransformer）：把"较长的对话历史 + 后续追问"压缩成一个独立、完整的查询，
 *    非常适合多轮对话场景（比如用户只问"那具体该怎么做？"）
 */
@Component
public class QueryTransformerDemo {

    private final QueryTransformer translationTransformer;
    private final QueryTransformer compressionTransformer;

    public QueryTransformerDemo(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 翻译转换器：目标语言中文
        this.translationTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(builder)
                .targetLanguage("chinese")
                .build();
        // 压缩转换器：把对话历史 + 追问压缩为独立查询
        this.compressionTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /** 查询翻译：英文问题 → 中文查询（适配中文 embedding 模型） */
    public String translateQuery(String prompt) {
        return translationTransformer.transform(new Query(prompt)).text();
    }

    /** 查询压缩：把对话历史和新问题压缩成一个独立完整的查询 */
    public String compressQuery(String question, List<Message> history) {
        Query query = Query.builder()
                .text(question)
                .history(history)
                .build();
        return compressionTransformer.transform(query).text();
    }
}
