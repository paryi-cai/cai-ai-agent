package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 向量存储配置 —— ETL 流程的后两步：Transform（转换）+ Load（加载）
 *
 * 原理：
 * 1. 文档块经过 Embedding 模型转换成高维向量（捕获语义特征）
 * 2. 向量 + 原文 + 元信息一起存入向量数据库，支持相似度搜索
 *
 * 这里使用 Spring AI 内置的 SimpleVectorStore（基于内存的向量库）：
 * - 优点：零依赖、开箱即用，适合学习
 * - 缺点：数据存在内存里，服务重启就丢失
 * 生产环境可以换成 PGVector、Milvus、Redis 等（VectorStore 接口不变，只换实现）
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     * 初始化恋爱大师向量库：项目启动时自动加载文档并向量化入库
     *
     * @param dashscopeEmbeddingModel Spring AI Alibaba 自动配置的通义千问嵌入模型
     */
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 构建内存向量库（Builder 模式，1.0 版本的写法）
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        // 加载文档：add() 内部会先调用 Embedding 模型把每个文档块转成向量，再存入向量库
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}
