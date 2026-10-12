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
 * 作用：创建"按区域过滤"的 RAG Advisor —— 检索时只搜指定区域的文档切片
 * 例如用户问南山的店，就只从"南山"文档里检索，避免召回福田/罗湖的无关内容
 *
 * 两个组件协作：
 * 1. FilterExpressionBuilder：构建元数据过滤表达式（语法类似 SQL）
 * 2. VectorStoreDocumentRetriever：带过滤条件 + 相似度阈值 + TopK 的文档检索器
 */
public class FoodAppRagCustomAdvisorFactory {

    /** 工具类，禁止实例化 */
    private FoodAppRagCustomAdvisorFactory() {
    }

    /**
     * 创建基于区域过滤的 RAG 检索增强 Advisor
     *
     * @param vectorStore 向量存储
     * @param region      区域（南山 / 福田 / 罗湖 / 宝安），只检索该区域的文档切片
     */
    public static Advisor createFoodAppRagCustomAdvisor(VectorStore vectorStore, String region) {
        // 过滤表达式：region == '南山'（作用在文档元数据上，先过滤再向量检索）
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("region", region)
                .build();
        // 文档检索器：限制过滤条件、相似度阈值、返回切片数量
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .topK(3)
                .build();
        // 检索增强 Advisor：把检索器挂到对话链上，自动完成"检索 + 查询增强"
        // 同时挂载自定义"空上下文增强器"：区域内检索不到内容时输出友好兜底话术（与云知识库链路行为一致）
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(FoodAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
