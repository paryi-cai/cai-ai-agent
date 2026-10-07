package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * PGVector 向量存储配置（真实向量数据库）
 *
 * 与 SimpleVectorStore（内存版）的区别：
 * - 数据持久化：向量存在 PostgreSQL 表里，重启不丢失
 * - 专业索引：支持 HNSW / IVFFlat 索引，大数据量下检索依然快
 * - 可运维：能直接 SQL 查询、备份、扩容
 *
 * 注意：这里不使用 Starter 自动配置，而是手动构造 PgVectorStore 对象
 * 原因：项目里已有多个 EmbeddingModel（Ollama/DashScope），自动注入会不知道选哪个而报错
 *
 * 前置条件（阿里云 RDS PostgreSQL）：
 * 1. 已创建数据库账号、数据库
 * 2. 已安装 vector 插件（插件管理 → vector）
 * 3. 已开通外网地址 + 白名单放行本机 IP
 * 4. application-local.yml 配置 spring.datasource.*
 */
@Configuration
public class PgVectorVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     * PGVector 向量存储 Bean
     * 注意：不在这里灌数据！建表逻辑在 Spring 生命周期回调（afterPropertiesSet）中执行，
     * @Bean 方法内部执行时表还没建好，所以数据初始化交给下面的 Runner 处理
     */
    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel,
                                           BatchingStrategy batchingStrategy) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .batchingStrategy(batchingStrategy)            // 自定义批处理策略（BatchingStrategyConfig）
                // ⚠️ 不要盲目指定 dimensions：不设置时自动从 Embedding 模型获取维度（设置错会报错）
                .distanceType(PgDistanceType.COSINE_DISTANCE)  // 距离算法：余弦相似度
                .indexType(PgIndexType.HNSW)                   // 索引类型：HNSW（高性能近似检索）
                .initializeSchema(true)                        // 自动建表（需要装 vector 插件）
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();
    }

    /**
     * 知识库数据初始化：仅当向量表为空时灌入文档（避免每次启动重复写入）
     * 用 Runner 的原因：它在整个容器启动完成后执行，此时 PgVectorStore 的建表初始化已完成
     */
    @Bean
    public ApplicationRunner pgVectorDataInitializer(JdbcTemplate jdbcTemplate,
                                                     @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore) {
        return args -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.vector_store", Integer.class);
            if (count != null && count > 0) {
                return;
            }
            List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
            // DashScope Embedding API 限制单次批量 ≤ 10 条，分批写入
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                pgVectorVectorStore.add(documents.subList(i, end));
            }
        };
    }
}
