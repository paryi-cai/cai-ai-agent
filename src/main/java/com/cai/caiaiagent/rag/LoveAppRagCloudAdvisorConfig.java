package com.cai.caiaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 云知识库 RAG 配置（阿里云百炼）
 *
 * 与本地知识库的区别：
 * - 本地：自己写 ETL（读文档 → 切分 → 向量化 → SimpleVectorStore），检索在内存里做
 * - 云端：文档上传、解析、切分、向量化、索引全部由百炼平台完成，代码只需调用"检索 API"
 *
 * 对接流程：
 * 1. DashScopeDocumentRetriever：调用百炼知识库的检索接口（按知识库名称检索）
 * 2. RetrievalAugmentationAdvisor：把"检索 + 查询增强"封装成 Advisor（1.0 包名：org.springframework.ai.rag.advisor）
 * 3. LoveApp.doChatWithRagCloud() 挂载使用
 */
@Configuration
@Slf4j
class LoveAppRagCloudAdvisorConfig {

    /** 复用配置里的百炼 API Key */
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    /** 云知识库名称（application.yml 中 love-app.rag.cloud-index-name 配置） */
    @Value("${love-app.rag.cloud-index-name}")
    private String cloudIndexName;

    @Bean
    public Advisor loveAppRagCloudAdvisor() {
        // 1.0 写法：DashScopeApi 从 new 改为 Builder 构建
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();

        // 云端文档检索器：内部调用百炼知识库检索接口
        // 更多可选参数：withDenseSimilarityTopK（数量）、withEnableReranking（重排序）、withSearchFilters（过滤）等
        DocumentRetriever documentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(cloudIndexName)
                        .build());

        // 检索增强 Advisor：调用前自动检索云端知识库，并把结果拼进提示词
        Advisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
        log.info("云知识库 Advisor 初始化完成，知识库名称：{}", cloudIndexName);
        return advisor;
    }
}
