package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多查询扩展演示（RAG 检索调优 - 预检索阶段的查询扩展）
 *
 * 原理：用 AI 把一个原始查询扩写成多个"语义变体"（类似搜索时换关键词），
 * 每个变体分别去向量库检索，最后合并去重 —— 提高相关文档的召回率。
 *
 * 完整流程共 3 步：
 * 1. 扩展：expand(原始查询) → 多个查询变体（默认包含原查询）
 * 2. 多路召回：遍历每个变体，分别做相似度检索
 * 3. 合并去重：ConcatenationDocumentJoiner 把多路结果拼成一份（重复只保留首次出现）
 *
 * ⚠️ 注意：会额外增加查询次数和 AI 调用成本，教程建议慎用（效果不易量化）
 */
@Component
public class MultiQueryExpanderDemo {

    private final MultiQueryExpander queryExpander;

    @Resource
    private VectorStore loveAppVectorStore;

    public MultiQueryExpanderDemo(ChatModel dashscopeChatModel) {
        // 构建扩展器：numberOfQueries(3) 表示扩展出 3 个变体（默认还会包含原始查询）
        this.queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(dashscopeChatModel))
                .numberOfQueries(3)
                .build();
    }

    /**
     * 把一个原始查询扩写成多个语义变体
     */
    public List<String> expandQuery(String question) {
        List<Query> queries = queryExpander.expand(new Query(question));
        return queries.stream().map(Query::text).toList();
    }

    /**
     * 完整流程：查询扩展 → 多路检索 → 合并去重
     */
    public List<Document> retrieveWithExpansion(String question) {
        // 1. 查询扩展
        List<Query> queries = queryExpander.expand(new Query(question));
        // 2. 每个变体分别检索（记录 Query -> 检索结果，供合并器使用）
        Map<Query, List<List<Document>>> documentsForQuery = new LinkedHashMap<>();
        for (Query query : queries) {
            List<Document> docs = loveAppVectorStore.similaritySearch(
                    SearchRequest.builder().query(query.text()).topK(3).build());
            documentsForQuery.put(query, List.of(docs));
        }
        // 3. 合并去重（ConcatenationDocumentJoiner：连接多路结果，重复文档保留首次出现）
        DocumentJoiner joiner = new ConcatenationDocumentJoiner();
        return joiner.join(documentsForQuery);
    }
}
