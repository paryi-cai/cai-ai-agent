package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 关键词元数据增强器测试
 * 注意：每个文档会调用一次 AI 提取关键词，本测试只放 1 个文档控制成本
 */
@SpringBootTest
class MyKeywordEnricherTest {

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Test
    void testEnrich() {
        List<Document> documents = List.of(new Document(
                "婚后感觉不如恋爱时亲密怎么办？建议每天 10 分钟专属聊天时间，每季度一次关系复盘。"));
        List<Document> enriched = myKeywordEnricher.enrichDocuments(documents);

        enriched.forEach(doc -> System.out.println("增强后 metadata：" + doc.getMetadata()));
        // 断言：AI 已把关键词补充进 metadata
        Assertions.assertFalse(enriched.get(0).getMetadata().isEmpty(), "应该补充了关键词元数据");
    }
}
