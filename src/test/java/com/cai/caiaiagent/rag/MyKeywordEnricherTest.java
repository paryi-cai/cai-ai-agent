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
                "深圳探店时怎么判断一家店值不值得去？建议先看评分分布和差评类型，再看菜单是否聚焦、有没有明确的招牌菜。"));
        List<Document> enriched = myKeywordEnricher.enrichDocuments(documents);

        enriched.forEach(doc -> System.out.println("增强后 metadata：" + doc.getMetadata()));
        // 断言：AI 已把关键词补充进 metadata
        Assertions.assertFalse(enriched.get(0).getMetadata().isEmpty(), "应该补充了关键词元数据");
    }
}
