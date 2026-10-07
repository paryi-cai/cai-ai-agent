package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * PGVector 向量存储测试
 *
 * 验证：文档写入 PostgreSQL → 相似度检索 → 返回带分数和元数据的结果
 * 前置条件：application-local.yml 已正确配置数据库连接
 */
@SpringBootTest
class PgVectorVectorStoreConfigTest {

    /** 按 Bean 名称注入 PGVector 版向量库（项目里同时存在内存版 loveAppVectorStore） */
    @Resource(name = "pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    @Test
    void testAddAndSearch() {
        // 写入 3 条测试文档（会自动调用 Embedding 转向量后落库）
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!",
                        Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.",
                        Map.of("meta2", "meta2")));
        pgVectorVectorStore.add(documents);

        // 相似度检索：查询 Spring
        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder().query("Spring").topK(5).build());

        // 断言：检索到结果，且带相似度分数
        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty(), "应该检索到相关文档");
        // 打印结果，观察分数和元数据
        results.forEach(doc -> System.out.println("命中: score=" + doc.getScore()
                + ", metadata=" + doc.getMetadata()
                + ", text=" + doc.getText().substring(0, Math.min(50, doc.getText().length())) + "..."));

        // 清理：删除本次测试写入的 3 条文档，避免重复运行导致数据堆积
        List<String> ids = documents.stream().map(Document::getId).toList();
        pgVectorVectorStore.delete(ids);
    }
}
