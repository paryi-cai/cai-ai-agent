package com.cai.caiaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 云向量存储演示（DashScopeCloudStore）
 *
 * 教程"支持的向量数据库"扩展知识：Spring AI Alibaba 把百炼知识库封装成了 VectorStore 实现，
 * 可以直接把云端知识库当作向量库使用（add/delete/similaritySearch 一应俱全）。
 *
 * 与 DashScopeDocumentRetriever 的区别：
 * - DocumentRetriever：只做检索，通常配 RetrievalAugmentationAdvisor 使用
 * - CloudStore：是完整的 VectorStore 实现，可以像本地向量库一样增删查
 */
@Component
public class DashScopeCloudStoreDemo {

    private final DashScopeCloudStore cloudStore;

    public DashScopeCloudStoreDemo(@Value("${spring.ai.dashscope.api-key}") String apiKey,
                                   @Value("${love-app.rag.cloud-index-name}") String indexName) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
        // 用百炼知识库名称构造云向量库
        this.cloudStore = new DashScopeCloudStore(dashScopeApi, new DashScopeStoreOptions(indexName));
    }

    /**
     * 直接对云知识库做相似度检索
     */
    public List<Document> search(String query, int topK) {
        return cloudStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build());
    }
}
