package com.cai.caiaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 查询重写器（RAG 预检索阶段的查询转换）
 *
 * 原理：用 AI 把"口语化 / 含糊 / 信息不全"的查询改写成更清晰、更利于检索的形式
 * 效果示例："结婚之后感觉感情淡了咋整" → "婚后夫妻亲密感下降该如何改善和经营？"
 *
 * 注意：每次对话会多一次 AI 调用（成本增加），可在检索质量差的场景按需启用
 */
@Component
public class QueryRewriter {

    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 查询重写转换器（底层就是给 AI 一段"请改写查询"的提示词）
        this.queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写
     *
     * @param prompt 原始查询
     * @return 重写后的查询（更适合检索）
     */
    public String doQueryRewrite(String prompt) {
        Query query = new Query(prompt);
        Query transformedQuery = queryTransformer.transform(query);
        return transformedQuery.text();
    }
}
