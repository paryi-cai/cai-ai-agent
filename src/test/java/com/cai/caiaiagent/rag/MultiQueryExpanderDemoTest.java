package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 多查询扩展测试
 * 消耗：每个测试 1 次 AI 扩写 + 数次向量检索（成本很低）
 */
@SpringBootTest
class MultiQueryExpanderDemoTest {

    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    /** 验证查询扩展效果：看一个模糊问题被扩写成什么样 */
    @Test
    void testExpandQuery() {
        String question = "婚后关系不亲密怎么办？";
        List<String> expanded = multiQueryExpanderDemo.expandQuery(question);
        System.out.println("原始查询：" + question);
        for (int i = 0; i < expanded.size(); i++) {
            System.out.println("扩展查询 " + (i + 1) + "：" + expanded.get(i));
        }
        Assertions.assertTrue(expanded.size() >= 2, "应该扩展出多个查询变体");
    }

    /** 验证完整流程：扩展 → 多路检索 → 合并去重 */
    @Test
    void testRetrieveWithExpansion() {
        List<Document> documents = multiQueryExpanderDemo.retrieveWithExpansion("婚后关系不亲密怎么办？");
        System.out.println("合并去重后的文档数：" + documents.size());
        documents.forEach(doc -> System.out.println("命中：" + doc.getMetadata().get("title")));
        Assertions.assertFalse(documents.isEmpty(), "应该检索到相关文档");
    }
}
