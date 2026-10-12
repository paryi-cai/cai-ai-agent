package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 摘要元数据增强器测试（1 次 AI 调用，仅 CURRENT）
 */
@SpringBootTest
class MySummaryEnricherTest {

    @Resource
    private MySummaryEnricher mySummaryEnricher;

    @Test
    void testEnrichBySummary() {
        List<Document> documents = List.of(new Document(
                "深圳探店时怎么判断一家店值不值得去？建议先看评分分布和差评类型，再看菜单是否聚焦、有没有明确的招牌菜。"));
        List<Document> enriched = mySummaryEnricher.enrichBySummary(documents);

        enriched.forEach(doc -> System.out.println("增强后 metadata：" + doc.getMetadata()));
        Assertions.assertFalse(enriched.get(0).getMetadata().isEmpty(), "应该补充了摘要元数据");
    }
}
