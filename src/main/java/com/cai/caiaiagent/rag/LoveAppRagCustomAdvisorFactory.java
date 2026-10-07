package com.cai.caiaiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 自定义 RAG 检索增强 Advisor 工厂
 *
 * 作用：创建"按恋爱状态过滤"的 RAG Advisor —— 检索时只搜指定状态的文档切片
 * 例如用户问婚后问题，就只从"已婚"文档里检索，避免召回单身/恋爱篇的无关内容
 *
 * 两个组件协作：
 * 1. FilterExpressionBuilder：构建元数据过滤表达式（语法类似 SQL）
 * 2. VectorStoreDocumentRetriever：带过滤条件 + 相似度阈值 + TopK 的文档检索器
 */
public class LoveAppRagCustomAdvisorFactory {

    /** 工具类，禁止实例化 */
    private LoveAppRagCustomAdvisorFactory() {
    }

    /**
     * 创建基于恋爱状态过滤的 RAG 检索增强 Advisor
     *
     * @param vectorStore 向量存储
     * @param status      恋爱状态（单身 / 恋爱 / 已婚），只检索该状态的文档切片
     */
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 过滤表达式：status == '已婚'（作用在文档元数据上，先过滤再向量检索）
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        // 文档检索器：限制过滤条件、相似度阈值、返回切片数量
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .topK(3)
                .build();
        // 检索增强 Advisor：把检索器挂到对话链上，自动完成"检索 + 查询增强"
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}
